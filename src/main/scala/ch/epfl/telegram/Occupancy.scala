package ch.epfl.telegram

import info.mukel.telegrambot4s._
import api._
import Implicits._
import java.net.URLEncoder


/**
  * Created by nghia on 30/11/16.
  */
trait Occupancy extends Commands {
  _ : TelegramBot =>

  on("/occupancy") { implicit msg => args =>
    reply(
      "http://occupancy.epfl.ch/" + URLEncoder.encode(args mkString " ", "UTF-8"),
      disableWebPagePreview = true
    )
  }
}


object Occupancy{
    val url = "http://occupancy.epfl.ch/"
}