package ch.epfl.telegram

import akka.http.scaladsl.server.{Directives, Route}
import info.mukel.telegrambot4s.api.TelegramBot

trait WebTelegramBot extends TelegramBot with Directives {

  def routes: Route = reject

}
