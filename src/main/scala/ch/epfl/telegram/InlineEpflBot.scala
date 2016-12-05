
import info.mukel.telegrambot4s._
import api._
import methods._
import models._
import Implicits._
import java.net.URLEncoder

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import ch.epfl.telegram.{DirectoryScraper, InlineEpflDirectory, Survey, TL, Events}

import scala.io.Source

object InlineEpflBot extends TelegramBot with Polling with Commands with ChatActions
  with TL with Survey with InlineEpflDirectory with Events {

  // PUT YOU TOKEN HERE
  lazy val token = Source.fromFile("token").getLines().mkString

  val ttsApiBase = "http://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=en-us&q="

  on("/speak") { implicit msg => args =>
    val text = args mkString " "
    val url = ttsApiBase + URLEncoder.encode(text, "UTF-8")
    for {
      response <- Http().singleRequest(HttpRequest(uri = Uri(url)))
      if response.status.isSuccess()
      bytes <-  Unmarshal(response).to[ByteString]
    } /* do */ {
      uploadingAudio // hint the user
      val voiceMp3 = InputFile.FromByteString("voice.mp3", bytes)
      request(SendVoice(msg.sender, voiceMp3))

      // Simple Java integration example for John.
      reply(MyFunctions.hello())
    }
  }

  on("/lmgtfy") { implicit msg => args =>
    reply(
      "http://lmgtfy.com/?q=" + URLEncoder.encode(args mkString " ", "UTF-8"),
      disableWebPagePreview = true
    )
  }

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

  def main(args: Array[String]): Unit = {
    //DirectoryScraper.getPersons("Sophia")
    run()
  }
}
