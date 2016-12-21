package ch.epfl.telegram

import info.mukel.telegrambot4s.api._

import scala.io.Source
import scala.util.Properties

object InlineEpflBot extends App with TelegramBot with Polling with Commands with ChatActions
  with TL with Survey with InlineEpflDirectory with Events with Menu with Room {

  lazy val token = Properties.envOrNone("EPFLBOT_TOKEN").getOrElse(Source.fromFile("token").getLines().mkString)

  on("/about") { implicit msg => _ =>
    reply(
      """
        |Hey!
        |
        |This bot offers various EPFL-specific campus services.
        |
        |The project is currently part of one SHS master class and aims at:
        |  - evaluating product creation processes
        |  - suggest new ways to interact within the campus
        |
        |Ping us for feedback and suggestions!
      """.stripMargin
    )
  }

  run()

}
