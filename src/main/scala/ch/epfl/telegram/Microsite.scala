package ch.epfl.telegram

import akka.http.scaladsl.server.{Directives, Route}
import info.mukel.telegrambot4s.api.{TelegramBot, WebRoutes}

trait Microsite extends WebRoutes {

  val port = Config.http.port

  override def routes: Route =
    super.routes ~
      pathEndOrSingleSlash {
        getFromResource("static/index.html")
      } ~
      getFromResourceDirectory("static")
}
