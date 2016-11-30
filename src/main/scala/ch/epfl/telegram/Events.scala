package ch.epfl.telegram

import info.mukel.telegrambot4s.api._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
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
    val events: List[Event] = EventsScrapper.eventsOfCategory(args(0).toInt)
     // TODO sanitize args // TODO retrieve the category to int mapping dynamically from page (in the button/menu div)
    events.foreach(event => reply(event.toString))  
  }
}

object EventsScrapper {
  val browser = JsoupBrowser()
  val baseUrl = "http://memento.epfl.ch/" 
  
  def eventsOfCategory(categoryID: Int) = {
    val doc = browser.get(baseUrl + "epfl/?category=" + categoryID)
    val eventDivs = doc >> elementList(".media.event")
    
    eventDivs.map(eventDiv => Event(eventDiv >> text("h2")))  
  }
}

case class Event(title: String) { /*,
                 description: String, 
                 url: String, 
                 location: String,
                 dateTime: String) {*/
  override def toString() = title  // TODO
}
