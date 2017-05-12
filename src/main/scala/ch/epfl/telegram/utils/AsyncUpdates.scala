package ch.epfl.telegram.utils

import info.mukel.telegrambot4s.api.{AkkaImplicits, BotBase}
import info.mukel.telegrambot4s.models.Update

import scala.concurrent.Future

/**
  * Make all updates async.
  */
trait AsyncUpdates extends BotBase with AkkaImplicits {
  override def onUpdate(u: Update): Unit = Future(super.onUpdate(u))
}
