package ch.epfl.telegram.utils

import info.mukel.telegrambot4s.api.TelegramBot
import info.mukel.telegrambot4s.marshalling.HttpMarshalling
import info.mukel.telegrambot4s.models.Update

trait LogUpdates extends TelegramBot {
  override def onUpdate(u: Update): Unit = {
    logger.debug(HttpMarshalling.toJson(u))
    super.onUpdate(u)
  }
}

