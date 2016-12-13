package ch.epfl.telegram

import info.mukel.telegrambot4s.api._

import scala.collection.JavaConverters._

trait Menu extends Commands { _: TelegramBot =>

  on("/menu") { implicit msg => args =>
    MenuHandler
      .search(msg.text.orNull, args.asJava)
      .asScala
      .foreach { rep =>
        reply(rep)
      }
  }
}
