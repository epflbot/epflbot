package ch.epfl.telegram

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import ch.epfl.telegram.commands._
import info.mukel.telegrambot4s.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.Properties

object EpflBot
    extends App
    with WebTelegramBot
    with Polling
    with Commands
    with ChatActions
    with TL
    with Bus
    with Survey
    with InlineEpflDirectory
    with Events
    with Menus
    with Room
    with About
    with Beers
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

  val bind = Http().bindAndHandle(routes, "::0", Config.http.port)
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
