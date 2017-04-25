package ch.epfl.telegram.commands

import info.mukel.telegrambot4s.api.{Commands, TelegramBot}
import info.mukel.telegrambot4s.methods.ParseMode

trait About extends Commands {
  _: TelegramBot =>

  on("/start", "welcome to @EPFLBot") { implicit msg =>
  {
    case Seq() =>
      reply(
        """
          |Welcome!
          |
          |This bot offers various EPFL-specific campus services to students and collaborators. It aims at providing interactive and social commands. Invite @EPFLBot into your favorite groups!
          |
          |You can use /help at any moment to list available commands, for instance:
          |  - /metro
          |  - /events
          |  - /menus
          |  - /room INF1
          |  - /feedback _I wish there was "meme" command!_
          |
          |Please take 2 minutes answering our /survey.
          |
          |Ping us for feedback and suggestions!
        """.stripMargin,
        parseMode = Some(ParseMode.Markdown)
      )
    case _ => /* ignore */
  }
  }

  on("/version", "@EPFLBot version") { implicit msg => _ =>
    import ch.epfl.telegram.version
    reply(s"EPFLBot v$version.")
  }

}