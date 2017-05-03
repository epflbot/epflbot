package ch.epfl.telegram.commands

import com.github.nscala_time.time.Imports._
import com.lightbend.emoji.ShortCodes.Defaults._
import com.lightbend.emoji.ShortCodes.Implicits._
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.methods.{EditMessageText, ParseMode}
import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import org.joda.time.{DateTime, Minutes}

import scala.util.Try

/**
  * Add TL (transport public Lausanne) useful commands.
  */
trait TL extends Commands with Callbacks {
  _ : TelegramBot =>

  private val TL_TAG = "TL_TAG"

  private val RunningTime = 8
  private val WalkingTime = 32

  val markup = {
    val renens = InlineKeyboardButton("Renens", callbackData = TL_TAG + "Renens")
    val flon = InlineKeyboardButton("Lausanne-Flon", callbackData = TL_TAG + "Lausanne-Flon")
    InlineKeyboardMarkup(Seq(Seq(renens, flon)))
  }

  /**
    * Crude command to get next next M1 departures, from EPFL, direction Flon.
    */
  on("/metro", "interactive metro schedule") { implicit msg => _ =>
    reply(horairesMessage("Lausanne-Flon"), parseMode = ParseMode.Markdown, replyMarkup = markup)
  }

  onCallbackWithTag(TL_TAG) { implicit cbq =>
    for {
      msg <- cbq.message
      dst <- cbq.data
    } /* do */ {
      val text = horairesMessage(dst)

      // Message must change!
      if (Option(text) != msg.text)
        request(
          EditMessageText(
            msg.chat.id,
            msg.messageId,
            text = text,
            parseMode = ParseMode.Markdown,
            replyMarkup = markup
          )
        )
    }
    ackCallback()
  }

  def horairesMessage(destination: String): String = {
    val now = DateTime.now

    val header = now.toString("HH:mm:ss") +  " " + "metro".emoji + " EPFL ➜ " + destination

    val text = TLScraper.horaires(destination).take(5).map { dt =>
      val remaining = Minutes.minutesBetween(now, dt).getMinutes()
      val s = dt.toString("HH:mm").bold + " "

      if (remaining <= RunningTime)
        s + "running".emoji // Hurry up!
      else if (remaining <= WalkingTime)
        s + "walking".emoji
      else
        s

    }.mkString("\n")

    val departures = if (text.isEmpty) "No departures in the next ~3 hours." else text
    header + "\n\n" + departures
  }
}

/**
  * TODO: Use https://github.com/RemembrMoe/tl-api
  */
object TLScraper {
  val browser = JsoupBrowser()

  val baseUrl = "http://www.t-l.ch/tl-live-mobile/"

  /**
    * Heavily hardcoded scraping. Find next M1 departures from EPFL, direction Flon (departing now).
    * @return List of next departures. Strings (HH:MM)
    */
  def horaires(direction: String, depart: DateTime = DateTime.now): Seq[DateTime] = {

    val directions = Map(
      "Lausanne-Flon" -> "line_detail.php?id=3377704015495518&line=11821953316814882&id_stop=2533279085549596&id_direction=11821953316814882",
      "Renens"        -> "line_detail.php?id=3377704015495520&line=11821953316814882&id_stop=2535851770776450&id_direction=11821953316814882"
    )

    directions.get(direction)
      .map(scrapDepartures(_))
      .getOrElse(Seq.empty)
  }

  private def scrapDepartures(path: String, depart: DateTime = DateTime.now): Seq[DateTime] = {
    val targetUrl = baseUrl + path +
        s"&jour=${depart.getYear}/${depart.getMonthOfYear}/${depart.getDayOfMonth}" +
        s"&heure=${depart.getHourOfDay}" +
        s"&minute=${depart.getMinuteOfHour}"

    val doc = browser.get(targetUrl)

    val Remaining = "(\\d+)'".r
    val HourMinutes = "(\\d+):(\\d+)".r

    val nextDepartures = doc >?> element("#lineDetailPage > div[data-role='content'] > div > ul")
    val departureTimes = nextDepartures.flatMap(_ >?> elements("> li > .time"))

    val times = for (lines <- departureTimes)
      yield {
        lines.toSeq.map(_.text) collect {
          case HourMinutes(AsInt(h), AsInt(m)) =>
            val t = depart.withHourOfDay(h).withMinuteOfHour(m)
            if (t < depart) t.plusDays(1) else t

          case Remaining(AsInt(m)) =>
            depart.plusMinutes(m)
        }
      }

    // There could be no departures in the next 3 hours (usually at night).
    times.getOrElse(Seq.empty)
  }

  private object AsInt {
    def unapply(s: String): Option[Int] = Try(s.toInt).toOption
  }
}