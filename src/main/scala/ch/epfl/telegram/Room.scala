package ch.epfl.telegram

import info.mukel.telegrambot4s._
import java.net.URLEncoder
import java.text.SimpleDateFormat

import util.control.Breaks._
import org.joda.time.{DateTime, Seconds}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import info.mukel.telegrambot4s.api.{Commands, TelegramBot}
import info.mukel.telegrambot4s.methods.{AnswerCallbackQuery, AnswerInlineQuery}
import info.mukel.telegrambot4s.api.{Commands, TelegramBot}
import scala.language.postfixOps
import scala.concurrent.{Await, Future, Promise}
import org.json4s._
import scala.concurrent.duration._
import org.json4s.jackson.JsonMethods._

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
    reply(responses)
  }
}
/*
case class Event(ResizeDisabled: Boolean,Tag: List[String],Start: Option[java.util.Date],
                 ClickDisabled: Boolean, Value : String, Resource: String, AllDay: Boolean,
                 BackColor : String, RecurrentMasterId: String, DeleteDisabled: Boolean,
                 End: Option[java.util.Date], DoubleClickDisabled: Boolean, Text: String, Recurrent: Boolean,
                 MoveDisabled: Boolean, Sort: String)
*/
case class Event(Tag: List[String], Start: java.util.Date, End: java.util.Date)


object Room{
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  def buildQuery(room: String) = s"https://ewa.epfl.ch/room/Default.aspx?room=$room"
  val MAGIC_TOKEN = "v.events = "
  val onlyToday = true // onlyToday = false => show all events from today to the end of the week

  def get(s : String) : String = {
    var result = ""

    val p = Promise[String]()

    for {
      response <- Http().singleRequest(HttpRequest(uri = Uri(buildQuery(s))))
      if response.status.isSuccess()
      html <- Unmarshal(response.entity).to[String]
      jsEventsLine <- html.split("\n").find(_.startsWith(MAGIC_TOKEN))
      json = jsEventsLine.drop(MAGIC_TOKEN.size).replaceAll("l\\\\","L")
    }
    {


      // Parse awkward JS datetimes
      implicit val formats = new DefaultFormats {
        override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
      }

      val events = parse(json).extract[Array[Event]]

    val startDay = new DateTime()  // Today
    var printedDay = startDay.minusDays(1)

    for (e <- events) {
      breakable {
        // Date of this event e
        val dateStartEvent = new DateTime(e.Start.getTime)
        val dateEndEvent = new DateTime(e.End.getTime)

        if (startDay.getDayOfYear > dateStartEvent.getDayOfYear)
          break // continue
        if (onlyToday & startDay.getDayOfYear < dateStartEvent.getDayOfYear)
          break // continue

        if (printedDay.getDayOfYear < dateStartEvent.getDayOfYear) {
          printedDay = dateStartEvent
          result += "-----------" + dateStartEvent.toDate.toString.substring(0, 10) + "-----------\n"
        }
        result += e.Tag(0).substring(0, e.Tag(0).length - 4) + "   ["
        result += dateStartEvent.toDate.toString.substring(11, 19) + "--"
        result += dateEndEvent.toDate.toString.substring(11, 19) + "]\n"
        }
      }

      p.success(if(result.length == 0) "No Course" else result)
    }
    Await.result(p.future, 5 second)
  }
}

