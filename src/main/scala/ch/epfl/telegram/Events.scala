package ch.epfl.telegram

import info.mukel.telegrambot4s.api._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import org.joda.time.DateTime

/**
  * Add Events (EPFL Events) useful commands.
  */
trait Events extends Commands {
  _ : TelegramBot =>

  /**
    * Crude command to get today EPFL events.
    */
  on("/events") { implicit msg => args =>
    // TODO sanitize args + 
    // offer way to search for events (new additional command or if no category found or ?) +
    val events: List[Event] = EventsScraper.retrieveEvents(EventsScraper.categoryOf(args(0)), args(1))
    events.foreach(event => reply(event.toString))  
  }
}

object EventsScraper {
  val browser = JsoupBrowser()
  val baseUrl = "http://memento.epfl.ch/" 
  
  // category=2 => Management Board meetings => no public events
  // dirty way to avoid loading a page full of events just to get the menu
  // (the whole design is already bad design) (lot of bloat retrieved on each user request) 
  // but don't want to change my get event code now ^^
  // FIXME if time maybe load the page with ALL events (+menu) once per day and do shit according to user input
  lazy val menu = 
    browser.get(baseUrl+"?category=2") >> elementList("div.toolbar-group:nth-child(2) > "+
                                                      "div:nth-child(1) > "+
                                                       "ul:nth-child(2) > "+
                                                       "li > a")
  lazy val categories = menu.map { element =>
    val name = element >> text("a")
    val id = element >> attr("href")("a")
    name.toLowerCase -> id
  }.toMap

  def categoryOf(arg: String): String =
    categories.find { 
      case (name, id) => name.startsWith(arg.toLowerCase) 
    }.getOrElse(("all..." -> ""))._2

  def parseEvent(eventDiv: Element): Event = {
    //general infos
    val titleElem = eventDiv >> element("h2 a")
    val title = titleElem >> attr("title")("a")
    val url = titleElem >> attr("href")("a")

    //practical infos
    val infosElem = eventDiv >> element(".media-info")
    val exportUrl = infosElem >> attr("href")("a")  // TODO not useful if only events of the day....
    //val dateTime = infosElem >> text("span.hour")
    //val location = infosElem >> text("span.location") 
    // FIXME WTF..seems to return but after this parseEvent does not return (seems). setup intelij, gedit + my brain are not enough...==> maybe javascript involved..
    // moreover dateTime is fine (but is retrieved in the "same" way)
    //println(location)
    val dateLocation = infosElem >> text(".media-info") // quick fix, see previous fixme, WTF why this works and no the other way

    // TODO description or url enough ?
    // TODO retrieve small image url

    Event(title, url, dateLocation, exportUrl)
  }
  
  def retrieveEvents(category: String, period: String): List[Event] = {
    val doc = browser.get(baseUrl + category + "&period=" + period)
    val eventDivs = doc >> elementList(".media.event")
    eventDivs.map(parseEvent(_))
  }
}

case class Event(title: String,
                 url: String, 
                 //location: String,
                 //dateTime: String,
                 dateLocation: String,
                 exportUrl: String) {
  override def toString() = "Title: " + title + "\n" +
							//"date time: " + dateTime + "\n" +
							//"location: " + location + "\n" + 
							"time, location: " + dateLocation + "\n" +
							"export ICal link: " + exportUrl + "\n" +
							"description: " + url
// TODO beautiful formatting....what does telegram allow to do ? link to epfl map + print image of event ? why not just render the whole <div> if allowed 
}
