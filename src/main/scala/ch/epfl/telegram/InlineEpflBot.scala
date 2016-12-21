package ch.epfl.telegram

import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.methods.ParseMode

import scala.io.Source
import scala.util.Properties

object InlineEpflBot extends App with TelegramBot with Polling with Commands with ChatActions
  with TL with Survey with InlineEpflDirectory with Events with Menu with Room {

  lazy val token = Properties.envOrNone("EPFLBOT_TOKEN").getOrElse(Source.fromFile("token").getLines().mkString)
  val version = "0.1.0"

  on("/start") { implicit msg => _ =>
    reply(
      """
        |Welcome!
        |
        |This bot offers various EPFL-specific campus services to students and collaborators. It aims at providing interactive and social commands. Invite @EPFLBot into your favorite groups!
        |
        |You can use /help at any moment to list available commands, for instance:
        |  - /metro
        |  - /feedback _I wish there was "meme" command!_
        |
        |Please take 2 minutes answering our /survey.
        |
        |Ping us for feedback and suggestions!
      """.stripMargin,
      parseMode = Some(ParseMode.Markdown)
    )
  }

  on("/version") { implicit msg => _ =>
    reply(s"EPFLBot v$version.")
  }

  run()

}
