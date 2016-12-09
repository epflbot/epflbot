package ch.epfl.telegram

import info.mukel.telegrambot4s.api._
import collection.JavaConverters._

trait Menu extends Commands {
  _ : TelegramBot =>

  on("/menu") { implicit msg => args =>
    for( i <- 0 to MenuHandler.search(msg.text.orNull, args.asJava).size()){
      reply(MenuHandler.search(msg.text.orNull, args.asJava).get(i));
    }

  }
}