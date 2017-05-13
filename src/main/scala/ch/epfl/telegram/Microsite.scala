package ch.epfl.telegram

import akka.http.scaladsl.server.Route
import info.mukel.telegrambot4s.api.WebRoutes

trait Microsite extends WebRoutes {

  import akka.http.scaladsl.server.Directives._

  val port = Config.http.port

  override def routes: Route =
    super.routes ~
      pathEndOrSingleSlash {
        getFromResource("static/index.html")
      } ~
      getFromResourceDirectory("static")
}
