package ch.epfl.telegram.commands

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import ch.epfl.telegram.models.{EPFLUser, TelegramInfo}
import ch.epfl.telegram.{Config, WebTelegramBot}
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api.Commands
import info.mukel.telegrambot4s.methods.{AnswerInlineQuery, GetMe}
import info.mukel.telegrambot4s.models._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

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
  * will provide an authentication link.
  *
  * If the authentication succeeds, the user will be redirected to the "?start=success" sub-command
  * to confirm that the login was successful.
  */
trait TequilaAuthentication extends WebTelegramBot with Commands { _: WebTelegramBot =>

  val tequilaToken = scala.collection.mutable.Map[String, TelegramInfo]() // request_token -> telegram_user_info

//  override def onMessage(message: Message): Unit = {
//    EPFLUser.fromId(message.from.map(_.id).getOrElse(0)).foreach {
//      case Some(_) => super.onMessage(message)
//      case None    => reply("Please /login first.")(message)
//    }
//  }

  on("/start") { implicit msg =>
    {
      case Seq("login") =>
        startLogin

      case Seq("success") =>
        // Greet the user
        for {
          user           <- msg.from
          linkedAccounts <- EPFLUser.fromTelegramId(user.id)
        } /* do */ {
          if (linkedAccounts.nonEmpty)
            reply(
              s"Hi, ${user.firstName}!\n" +
                "You are successfully authenticated with your EPFL account.\n" +
                "@EPFLBot DO NOT store any sensible information such as passwords or personal data.",
              replyMarkup = ReplyKeyboardRemove()
            )
          else
            reply("Your /login attempt failed.")
        }

      case _ => /* ignore */
    }
  }

  on("/login", "link your EPFL account") { implicit msg => _ =>
    startLogin
  }

  on("/logout", "unlink your EPFL account") { implicit msg => _ =>
    for (user <- msg.from) {
      for (linkedAccounts <- EPFLUser.fromTelegramId(user.id)) {
        EPFLUser.putUserSeq(linkedAccounts.map(_.copy(telegramInfo = None)))
        reply("Bye bye!")
      }
    }
  }

  on("/status", "shows authentication status") { implicit msg => _ =>
    for {
      user           <- msg.from
      linkedAccounts <- EPFLUser.fromTelegramId(user.id)
    } /* do */ {
      if (linkedAccounts.isEmpty)
        reply(
          "You have no EPFL account linked.\n" +
            "Please /login first.")
      else {
        val desc = for (account <- linkedAccounts)
          yield account.email
        reply(
          "You have linked the following EPFL account(s):\n" +
            desc.mkString("\n"))
      }
    }
  }

  // In order to search the EPFL directory user must be authenticated.
  override def onInlineQuery(inlineQuery: InlineQuery): Unit = {
    for (linkedAccounts <- EPFLUser.fromTelegramId(inlineQuery.from.id)) {
      // If the user have an EPFL account associated, let the query pass
      // to the upper level (InlineEPFLDirectory).
      if (linkedAccounts.nonEmpty)
        super.onInlineQuery(inlineQuery)
      else {
        // Otherwise block the inline query and ask the user to authenticate
        // using his EPFL account.

        // This only works on Telegram mobile (tested on Android)
        // TODO: Report to Telegram support
        request(
          AnswerInlineQuery(inlineQuery.id,
                            cacheTime = 1,
                            switchPmText = "Link your EPFL account!",
                            results = Seq.empty,
                            switchPmParameter = "login")
        )
      }
    }
  }

  def startLogin(implicit msg: Message): Unit = {
    for (user <- msg.from) {
      if (msg.chat.`type` != "private") {
        val privateChatUrl = deepLink("login").toString()
        val button         = InlineKeyboardMarkup(Seq(Seq(InlineKeyboardButton("Let's do it!", url = privateChatUrl))))

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
              tequilaToken(key) = TelegramInfo(user.id, user.username)
            }

            reply(s"Open this link to authenticate:\n ${uri.toString()}", replyMarkup = ReplyKeyboardRemove())
          }
        }
      }
    }
  }

  val host             = Config.tequila.host
  val createRequestUri = s"$host/createrequest"
  val requestAuthUri   = s"$host/requestauth"
  val fetchAttrsUri    = s"$host/fetchattributes"
  val tokenUri         = "tequila"

  // TODO: Put this global in EPFLBot?
  private lazy val me = Await.result(request(GetMe), 10 seconds)
  val botName         = me.username.get

  val deepLinkUri = s"https://t.me/$botName" // ?start=

  private val createRequestParams = {
    val interface = Config.http.interface
    val port      = Config.http.port

    val params = Map(
      "urlaccess" -> s"$interface:$port/$tokenUri",
      "service"   -> Config.tequila.service,
      "request"   -> Config.tequila.request,
      "require"   -> Config.tequila.require,
      "allows"    -> Config.tequila.allows
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
    data.foldLeft("") {
      case (content, (key, value)) =>
        s"$content\n$key=$value"
    }

  def tequilaDeserialize(data: String): Map[String, String] =
    data
      .split('\n')
      .flatMap { entry =>
        entry.split('=').toList match {
          case k :: v :: Nil => Some(k -> v)
          case _             => None
        }
      }
      .toMap

  override abstract def routes: Route = super.routes ~ {
    pathPrefix(tokenUri) {
      parameters("key") { key =>
        val redirection = for {
          req <- Http().singleRequest(fetchAttrs(key.trim()))
          res <- Unmarshal(req.entity).to[String]
        } yield {
          val data = tequilaDeserialize(res)

          val telegramInfoOpt = tequilaToken.synchronized { tequilaToken.get(key) }
          val sciper          = data("uniqueid").toInt

          val updateTelegramInfo =
            for (telegramInfo <- telegramInfoOpt) yield {
              EPFLUser.fromId(sciper).flatMap {
                case Some(epflUser) =>
                  EPFLUser.putUser(epflUser.copy(telegramInfo = Some(telegramInfo)))

                case None => Future.successful(())
              }
            }

          // Blocking is inevitable.
          // This should be rather (also) on a memory backed storage.
          updateTelegramInfo.foreach { Await.result(_, 5.seconds) }

          redirect(deepLink("success"), StatusCodes.TemporaryRedirect)
        }

        Await.result(redirection, 10 seconds)
      }
    }
  }

}
