package ch.epfl.telegram

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import ch.epfl.telegram.commands._
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.methods.ParseMode

import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.Properties
import scala.concurrent.duration._

object EpflBot
    extends App
    with WebTelegramBot
    with Polling
    with Commands
    with ChatActions
    with TL
    with Survey
    with InlineEpflDirectory
    with Events
    with Menus
    with Room
    with About
    // with AddYourCoolFeatureHere ...

    /* The access-control trait must be the last */
    with TequilaAuthentication {

  lazy val token = Properties
      .envOrNone("EPFLBOT_TOKEN")
      .getOrElse(Source.fromFile("token").getLines().mkString)

  override def routes: Route =
    super.routes ~
      pathEndOrSingleSlash {
        getFromResource("static/index.html")
      } ~
      getFromResourceDirectory("static")

  val bind = Http().bindAndHandle(routes, "::0", 8080)
  bind.foreach { _ =>
    logger.info("EPFL bot just started.")
  }

  sys.addShutdownHook {
    val stops = List(
      bind.flatMap(_.unbind()),
      system.terminate()
    )
    Await.ready(Future.sequence(stops), 30.seconds)
  }

  run()

}
