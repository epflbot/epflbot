package ch.epfl.telegram.commands

import com.lightbend.emoji.ShortCodes.Defaults._
import com.lightbend.emoji.ShortCodes.Implicits._
import info.mukel.telegrambot4s.Implicits.Extractor
import info.mukel.telegrambot4s.api.{Callbacks, ChatActions, Commands, TelegramBot}
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.methods.{EditMessageText, ParseMode}
import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element, elements}
import org.joda.time.DateTime

/**
  * Add bus (Transport Region Morges) useful commands.
  */
trait Bus extends Commands with Callbacks {
  _: TelegramBot with ChatActions =>

  private val BUS_TAG = "BUS_TAG"

  private def tag(text: String): String = prefixTag(BUS_TAG)(text)

  private val RunningTime = 8
  private val WalkingTime = 32

  val supportedBuses = Seq(701, 705)

  val busCode =
    Map(701 -> Map("Morges − Echichens" -> 711, "Bourdonnette" -> 683), 705 -> Map("Lonay" -> 717, "Piccard" -> 716))

  val busSrcDest = Map(701 -> (("Morges − Echichens", "Bourdonnette")), 705 -> (("Lonay", "Piccard")))

  val epflStops = Map(701 -> "Parc Scient.", 705 -> "EPFL")

  /**
    * Crude command to get next bus departures.
    */
  on("/bus", "interactive 701 and 705 bus schedule") { implicit msg =>
    {
      case Seq(Extractor.Int(busNumber)) if supportedBuses contains busNumber =>
        typing
        reply(horairesMessage(busNumber, busCode(busNumber).head._1),
              parseMode = Some(ParseMode.Markdown),
              replyMarkup = Some(markup(busNumber)))

      case _ =>
        val buttons = supportedBuses.map { b =>
          InlineKeyboardButton("oncoming_bus".emoji + " " + b, callbackData = tag(b + "#" + busSrcDest(b)._1))
        }

        reply("Pick your bus", replyMarkup = InlineKeyboardMarkup(Seq(buttons)))
    }
  }

  onCallbackWithTag(BUS_TAG) { implicit cbq =>
    for {
      msg  <- cbq.message
      data <- cbq.data
    } /* do */ {
      val dataList  = data.split("#")
      val busNumber = dataList.head.toInt
      val dest      = dataList.tail.head
      val text      = horairesMessage(busNumber, dest)

      // Message must change!
      if (Option(text) != msg.text)
        request(
          EditMessageText(
            msg.chat.id,
            msg.messageId,
            text = text,
            parseMode = ParseMode.Markdown,
            replyMarkup = markup(busNumber)
          )
        )
    }
    ackCallback()
  }

  def markup(busNumber: Int): InlineKeyboardMarkup = {
    val source =
      InlineKeyboardButton(busSrcDest(busNumber)._1, callbackData = tag(busNumber + "#" + busSrcDest(busNumber)._1))

    val dest = InlineKeyboardButton(busSrcDest(busNumber)._2,
                                    callbackData = Some(tag(busNumber + "#" + busSrcDest(busNumber)._2)))
    InlineKeyboardMarkup(Seq(Seq(source, dest)))
  }

  def horairesMessage(busNumber: Int, destination: String): String = {
    val now = DateTime.now

    val header = now.toString("HH:mm:ss") + " " + "bus".emoji + " Bus: " + busNumber +
      "\nEPFL ➜ " + destination

    val code     = busCode(busNumber)(destination)
    val stopName = epflStops(busNumber)

    val text = BusScraper
      .scrapDepartures(code, stopName)
      .take(5)
      .map { dt =>
        val s = dt + "'" + " "

        if (dt <= RunningTime)
          s + "running".emoji // Hurry up!
        else if (dt <= WalkingTime)
          s + "walking".emoji
        else
          s
      }
      .mkString("\n")

    val departures = if (text.isEmpty) "No departures in the next ~3 hours." else text
    header + "\n\n" + departures
  }
}

/**
  * Scrap transport region Morges
  */
object BusScraper {

  val baseUrl = "https://mbc.ch/?post_type=ligne&p="

  def scrapDepartures(dir: Int, stopName: String): Seq[Int] = {

    val doc = JsoupBrowser().get(baseUrl + dir)

    val Remaining = "~?(\\d+)'".r

    val nextDepartures = doc >> element(".table > tbody:nth-child(1)")

    val t = for (tb <- nextDepartures >> elements("> tr"))
      yield tb.flatMap(_ >> texts("> td")) filter (x => !x.isEmpty)

    val departures = t.filter(x => x.head == stopName).flatMap(t => t.tail)

    val times = departures collect {
      case Remaining(Extractor.Int(m)) =>
        m
    }

    times.toSeq
  }

}
