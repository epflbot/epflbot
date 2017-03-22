package ch.epfl.telegram

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.config.ConfigFactory
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api.{BotBase, Commands, TelegramBot}
import info.mukel.telegrambot4s.methods.{AnswerInlineQuery, GetMe}
import info.mukel.telegrambot4s.models._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

case class EPFLUser(sciper: Int, firstName: String, name: String, email: String, gaspar: String, where: String)

/**
  * EPFL's Tequila authentication, the process is as follows:
  *
  * To initiate the authentication:
  *   - Use /login command
  *   - Use inline search @epflbot
  *
  * Authentication should always happen in a private chat with @EPFLBot.
  * In the case that the login request happens in a non-private chat, or inline search,
  * the user will be asked to switch to a private chat.
  *
  * To take advantage of deep-linking, there are two "sub-commands":
  *   - ?start=login
  *   - ?start=success
  *
  * The "login" subcommand will get a requestKey from EPFL's Tequila servers, and
  * will provide an autnehtication link.
  *
  * If the authentication succeeds, the user will be redirected to the "?start=success" sub-command
  * to confirm that the login was successful.
  */
trait TequilaAuthentication extends BotBase with Commands {
  _: TelegramBot =>

  // TODO: Persist to database
  val loggedUsers = scala.collection.mutable.Map[Int, EPFLUser]() // telegram_user_id -> sciper
  val tequilaToken = scala.collection.mutable.Map[String, Int]() // request_token -> telegram_user_id

  on("/start") { implicit msg => {
    case Seq("login") =>
      startLogin

    case Seq("success") =>
      // Greet the user
      for (user <- msg.from) {
        for (epflUser <- loggedUsers.synchronized { loggedUsers.get(user.id)})
          reply(s"Hi, ${epflUser.firstName}!\n" +
            "You are successfully authenticated with your EPFL account.\n" +
            "@EPFLBot DO NOT store any sensible information such as passwords or personal data.",
            replyMarkup = ReplyKeyboardRemove())
      }

    case _ => /* ignore */
  }
  }

  on("/login") { implicit msg => _ =>
    startLogin
  }

  on("/logout") { implicit msg => _ =>
    for (user <- msg.from) {
      loggedUsers.synchronized {
        loggedUsers -= user.id
        reply("Bye bye!")
      }
    }
  }


  override def onInlineQuery(inlineQuery: InlineQuery): Unit = {

    val epflUser = loggedUsers.synchronized {
      loggedUsers.get(inlineQuery.from.id)
    }

    if (epflUser.isEmpty) {
      // This only works on Telegram mobile (tested on Android)
      // TODO: Report to Telegram support
      request(
        AnswerInlineQuery(
          inlineQuery.id,
          cacheTime = 1,
          switchPmText = "Connect to your EPFL account!",
          results = Seq.empty,
          switchPmParameter = "login"))
    } else
      super.onInlineQuery(inlineQuery)
  }

  def startLogin(implicit msg: Message): Unit = {
    for (user <- msg.from) {
      val userIsLoggedIn = loggedUsers.synchronized {
        loggedUsers.contains(user.id)
      }
      if (userIsLoggedIn)
        reply("You are already logged in, please /logout first.")
      else {
        if (msg.chat.`type` != "private") {
          val privateChatUrl = deepLink("login").toString()

          val button = InlineKeyboardMarkup(Seq(Seq(
            InlineKeyboardButton("Let's do it!", url = privateChatUrl))))

          reply("Switch to private chat to login.", replyMarkup = button)
        } else {

          for {
            req <- Http().singleRequest(createRequest)
            res <- Unmarshal(req.entity).to[String]
          } yield {
            if (!res.startsWith("key=")) {
              throw new Exception(s"bad tequila create request response: '$res'")
            } else {
              val key = res.drop(4).trim()
              val uri = requestAuth(key)

              tequilaToken.synchronized {
                tequilaToken(key) = user.id
              }

              reply(s"Open this link to authenticate:\n ${uri.toString()}", replyMarkup = ReplyKeyboardRemove())
            }
          }
        }
      }
    }
  }

  import Directives._

  val host = Config.tequila.host
  val createRequestUri = s"$host/createrequest"
  val requestAuthUri = s"$host/requestauth"
  val fetchAttrsUri = s"$host/fetchattributes"
  val tokenUri = "tequila"

  // TODO: Put this global in EPFLBot?
  private lazy val me = Await.result(request(GetMe), 10 seconds)
  val botName = me.firstName

  val deepLinkUri = s"https://t.me/$botName" // ?start=

  private val createRequestParams = {
    val interface = Config.http.interface
    val port = Config.http.port

    val params = Map(
      "urlaccess" -> s"http://$interface:$port/$tokenUri",
      "service" -> Config.tequila.service,
      "request" -> Config.tequila.request,
      "require" -> Config.tequila.require,
      "allows" -> Config.tequila.allows
    )
    tequilaSerialize(params)
  }

  val error = StatusCodes.InternalServerError -> "tequila error"

  def createRequest: HttpRequest =
    RequestBuilding.Post(createRequestUri, createRequestParams)

  def deepLink(data: String): Uri =
    Uri(deepLinkUri).withQuery(Query("start" -> data))

  def requestAuth(key: String): Uri =
    Uri(requestAuthUri).withQuery(Query("requestkey" -> key))

  def fetchAttrs(key: String): HttpRequest =
    RequestBuilding.Post(fetchAttrsUri, s"key=$key")

  def tequilaSerialize(data: Map[String, String]): String =
    data.foldLeft("") { case (content, (key, value)) =>
      s"$content\n$key=$value"
    }

  def tequilaDeserialize(data: String): Map[String, String] =
    data.split('\n').flatMap { entry =>
      entry.split('=').toList match {
        case k :: v :: Nil => Some(k -> v)
        case _ => None
      }
    }.toMap

  val routes =
    pathPrefix(tokenUri) {
      parameters("key") { key =>
        val redirection = for {
          req <- Http().singleRequest(fetchAttrs(key.trim()))
          res <- Unmarshal(req.entity).to[String]
        } yield {
          val data = tequilaDeserialize(res)

          val epflUser = EPFLUser(
            sciper = data("uniqueid").toInt,
            firstName = data("firstname"),
            name = data("name"),
            email = data("email"),
            gaspar = data("user"),
            where = data("where"))

          loggedUsers.synchronized {
            tequilaToken.synchronized {
              for (token <- tequilaToken.get(key))
                loggedUsers += (token -> epflUser)
            }
          }

          redirect(deepLink("success"), StatusCodes.TemporaryRedirect)
        }

        Await.result(redirection, 10 seconds)
      }
    }

  override abstract def run(): Unit = {
    super.run()
    // TODO: Fix this to work on the server
    Http().bindAndHandle(routes, "::0", 8080)
  }
}

