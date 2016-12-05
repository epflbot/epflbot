package ch.epfl.telegram

import info.mukel.telegrambot4s._
import java.net.URLEncoder
import java.text.SimpleDateFormat

import org.joda.time.DateTime
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer

import info.mukel.telegrambot4s.api.{Commands, TelegramBot}
import info.mukel.telegrambot4s.methods.{AnswerCallbackQuery, AnswerInlineQuery}
import info.mukel.telegrambot4s.api.{Commands, TelegramBot}

import scala.concurrent.{Future, Promise}

/**
  * Created by nghia on 30/11/16.
  * This is a version that "work', I will optimal after
  * In Telegram, try with arguments like "/room co021", "/room bc01"
  * Source original = "https://ewa.epfl.ch/room/Default.aspx?room=bc01"
  * Still not deal with  wrong argument....
  */
trait Room extends Commands {
  _ : TelegramBot =>

  on("/room") { implicit msg => args =>
    val responses =  Room.get(URLEncoder.encode(args mkString " ", "UTF-8"))
    for (response <- responses)
      reply(response)
  }
}
/*
case class Event(ResizeDisabled: Boolean,Tag: List[String],Start: Option[java.util.Date],
                 ClickDisabled: Boolean, Value : String, Resource: String, AllDay: Boolean,
                 BackColor : String, RecurrentMasterId: String, DeleteDisabled: Boolean,
                 End: Option[java.util.Date], DoubleClickDisabled: Boolean, Text: String, Recurrent: Boolean,
                 MoveDisabled: Boolean, Sort: String)
*/
case class Event(Tag: List[String], Start: Option[java.util.Date], End: Option[java.util.Date])


object Room{
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  def buildQuery(room: String) = s"https://ewa.epfl.ch/room/Default.aspx?room=$room"
  val MAGIC_TOKEN = "v.events = "

  def get(s : String) : Future[String] = {
    var result = ""

    val p = Promise[String]()

    for {
      response <- Http().singleRequest(HttpRequest(uri = Uri(buildQuery(s))))
      if response.status.isSuccess()
      html <- Unmarshal(response.entity).to[String]
      jsEventsLine <- html.split("\n").find(_.startsWith(MAGIC_TOKEN))
      json = jsEventsLine.drop(MAGIC_TOKEN.size)
    } {
      import org.json4s._
      import org.json4s.jackson.JsonMethods._

      // Parse awkward JS datetimes
      implicit val formats = new DefaultFormats {
        override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
      }

      val events = parse(json).extract[Array[Event]]

     // events foreach println
      /**/

      var beginDate = new DateTime(events(0).Start.getOrElse(null).getTime)
      result += "-----------"+beginDate.toDate.toString.substring(0, 10) + "-----------\n"
      for (e <- events) {
        // Date of this event
        val dateStartEvent = new DateTime(e.Start.getOrElse(null).getTime)
        val dateEndEvent = new DateTime(e.End.getOrElse(null).getTime)
        if (beginDate.getDayOfYear != dateStartEvent.getDayOfYear) { // If there is a change in date
          beginDate = dateStartEvent
          result += "-----------"+beginDate.toDate.toString.substring(0, 10) + "-----------\n"
        }
        result = result.concat(e.Tag(0).substring(0, e.Tag(0).length-4))+" ["
        result = result.concat(dateStartEvent.toDate.toString.substring(11,19)) +"--"
        result = result.concat(dateEndEvent.toDate.toString.substring(11,19)+"]\n")

      }

      p.success(result)
    }


    p.future
  }
}