package ch.epfl.telegram

import info.mukel.telegrambot4s.api._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import org.joda.time.DateTime

/**
  * Add TL (transport public Lausanne) useful commands.
  */
trait TL extends Commands { _: TelegramBot =>

  /**
    * Crude command to get next next M1 departures, from EPFL, direction Flon.
    */
  on("/metro") { implicit msg => _ =>
    reply(TLScraper.horaires_M1_EPFL_Flon() mkString "\n")
  }
}

/**
  * TODO: Use https://github.com/RemembrMoe/tl-api
  */
object TLScraper {
  val browser = JsoupBrowser()

  val baseUrl = "http://www.t-l.ch/tl-live-mobile/"

  def getLines() = {
    val doc   = browser.get(baseUrl + "index.php")
    val lines = doc >> element("#page_horaire > div:nth-child(2) > ul") >> elements("> li > a")
    lines map (_.attr("href"))
  }

  /**
    * Heavily hardcoded scraping. Find next M1 departures from EPFL, direction Flon (departing now).
    * @return List of next departures. Strings (HH:MM)
    */
  def horaires_M1_EPFL_Flon() = {
    val now = DateTime.now
    val timeParams =
      s"""jour=${now.getYear}%2F${now.getMonthOfYear}%2F${now.getDayOfMonth}&heure=${now.getHourOfDay}&minute=${now.getMinuteOfHour}"""
    val doc = browser.get(
      baseUrl + "line_detail.php?id=3377704015495518&line=11821953316814882&id_direction=11821953316814882&id_stop=2533279085551931&" + timeParams)
    val lines = doc >> element("#lineDetailPage > div:nth-child(4) > div > ul") >> elements("> li > .time")
    lines map (_.text)
  }
}
