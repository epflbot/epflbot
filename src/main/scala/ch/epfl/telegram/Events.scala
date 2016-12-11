package ch.epfl.telegram

import info.mukel.telegrambot4s.api._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import org.joda.time.{DateTime, LocalDate, LocalTime, Period, Duration}
import org.joda.time.format.{PeriodFormatterBuilder, DateTimeFormatterBuilder, DateTimeFormat}

/**
  * Add Events (EPFL Events) useful commands.
  */
trait Events extends Commands {
  _ : TelegramBot =>

  /**
    * Crude command to get today EPFL events.
    */
  on("/events") { implicit msg => _ =>
    // offer way to search for events (new additional command or if no category found or ?) +
    val events: List[Event] = EventsScraper.retrieveEvents("?", 1)
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
    }.getOrElse(("all..." -> "?"))._2

  def parseEvent(eventDiv: Element): Event = {
    // Title, URL
    val titleElem = eventDiv >> element("h2 a")
    val title = titleElem >> attr("title")("a")
    val url = titleElem >> attr("href")("a")

    // ICal
    val infosElem = eventDiv >> element(".media-info")
    val exportUrl = infosElem >> attr("href")("a")  // TODO not useful if only events of the day....
    // Time
    val times = (infosElem >> text(".hour")).split("-").toList
    val startTime = times.headOption.getOrElse("00:00")  // not really used, (seems to be always already part of startDate)
    val endTime = times.tail.headOption.getOrElse("23:59")  // on the other hand the endTime is not part of the endDate
    val (endHour, endMin) = (endTime.substring(0, 2).toInt, endTime.substring(3).toInt)

    val dateTimeElem = eventDiv >> element(".media-ribbon.local-bg-color1")
    val startDate = DateTime.parse(dateTimeElem >> attr("content") ("meta:first-of-type"))
    val endDate = DateTime.parse(dateTimeElem >> attr("content") ("meta:last-of-type")).withHourOfDay(endHour).withMinuteOfHour(endMin)  // the end/start date are used to hold the times, see Event#isValidToShow()
    // Location
    val locationElem = infosElem >> element("a + span")
    val location = locationElem.text match {
      case "" => None
      case string => Some(string)
    }
    val locationUrl = (locationElem >?> attr("href") ("a"))

    // TODO retrieve small image url

    Event(title, url, startDate, endDate, location, locationUrl, exportUrl, eventDiv)
  }
  
  def retrieveEvents(category: String, period: Int): List[Event] = {
    val doc = browser.get(baseUrl + category + "&period=" + period)
    val eventDivs = doc >> elementList(".media.event")
    eventDivs.map(parseEvent(_)).filter(_.isValidToShow())
  }
}

case class Event(title: String,
                 url: String, 
                 startDate: DateTime,
                 endDate: DateTime,
                 location: Option[String],
                 locationUrl: Option[String],
                 exportUrl: String,
                 eventDiv: Element)  // just in case
{
  def isValidToShow() = {
    val (today, time) = (LocalDate.now, LocalTime.now)
    val startDay = startDate.toLocalDate()
    val (endDay, endTime) = (endDate.toLocalDate(), endDate.toLocalTime())

    val isToday = (today.isAfter(startDay) || today.isEqual(startDay)) &&
                  (today.isBefore(endDay) || today.isEqual(endDay))
    isToday && time.isBefore(endTime)
  }

  override def toString() = {
    val periodFmter = new PeriodFormatterBuilder().appendDays().appendSuffix(" day", " days")
                                                .appendSeparator(" and ")
                                                .appendHours().appendSuffix(" hour", " hours")
                                                .appendSeparator(" and ")
                                                .appendMinutes().appendSuffix(" minute", " minutes")
                                                .appendSeparator(" and ")
                                                .appendSeconds().appendSuffix(" second", " seconds")
                                                .toFormatter()
    val dateFmter = new DateTimeFormatterBuilder().appendDayOfWeekText()
                                                  .appendLiteral(", ")
                                                  .appendMonthOfYearText()
                                                  .appendLiteral(' ')
                                                  .appendDayOfMonth(2)
                                                  .appendLiteral(' ')
                                                  .appendYear(2, 4)
                                                  .toFormatter()
    val timeFmter = DateTimeFormat.forPattern("HH:mm")
    val remaining = (new Duration(LocalTime.now.getMillisOfDay(), endDate.toLocalTime().getMillisOfDay())).toPeriod()

    title + "\n\n" +
    dateFmter.print(startDate.toLocalDate()) + " -to- " + dateFmter.print(endDate.toLocalDate()) + "\n" +
    "From: " + timeFmter.print(startDate.toLocalTime()) + " Until: " + timeFmter.print(endDate.toLocalTime()) +
      " (" + periodFmter.print(remaining) + " remaining) " + "\n" +
    ((location, locationUrl) match {
      case (Some(loc), Some(locUrl)) => "At: " + loc + " (" + locUrl + ")\n\n"
      case (Some(loc), None) => "At: " + loc + "\n\n"
      case _ => ""
    }) +
    "export ICal link: " + exportUrl + "\n" +
    "description: " + url
  }
  // override def toString() = eventDiv.toString() HTML ?...
  // TODO beautiful formatting....what does telegram allow to do ? print image of event ? why not just render the whole <div> if allowed
}

