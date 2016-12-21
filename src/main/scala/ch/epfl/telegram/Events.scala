package ch.epfl.telegram

import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.models._
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.methods.{EditMessageText, ParseMode}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import org.joda.time.{DateTime, Duration, LocalDate, LocalTime, Period}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatterBuilder, PeriodFormatterBuilder}

/**
  * Add Events (EPFL Events) useful commands.
  */
trait Events extends Commands with Callbacks { _ : TelegramBot =>

  import Events.callbackPrefix

  /**
    * Crude command to get today EPFL events. (that are not "already finished")
    */
  on("/events") { implicit msg => _ =>
    // TODO offer way to search for events
    Events.events = EventsScraper.retrieveEvents("?", 1)  // TODO DIRTY !!!!!!! ^^
    Events.events match {
      case event::_ => reply(
        event.toString(),
        replyMarkup = Events.getKeyboard(0),
        parseMode = ParseMode.Markdown
      )
      case _ => reply("nothing to show")
    }

  }

  onCallbackWithTag(callbackPrefix) {
    case clb @ CallbackQuery(_, _, Some(message), _, _, Some(data), _) =>
      logger.debug("callback query data {}", data)

      data.stripPrefix(callbackPrefix).toInt match {
        case -1 => request (
          EditMessageText (
            messageId = message.messageId,
            chatId = message.chat.id,
            text = message.text.getOrElse("nothing to show"),  // TODO keep formatting
            parseMode = ParseMode.Markdown
          )
        )
        case index => request (
          EditMessageText (
            messageId = message.messageId,
            chatId = message.chat.id,
            text = Events.events(index).toString,
            replyMarkup = Events.getKeyboard(index),
            parseMode = ParseMode.Markdown
          )
        )
      }

      /*val (msgText, keyboard) = data.replace(Events.callbackPrefix, "").toInt match {
        case -1 => (message.text.getOrElse("nothing to show"), None)
        case index => (Events.events(index).toString, Some(Events.getKeyboard(index)))
      }
      EditMessageText (
        messageId = message.messageId,
        chatId = message.chat.id,
        text = msgText,
        replyMarkup = keyboard,
        parseMode = ParseMode.Markdown
      )*/ // TODO why does not work this way ?
      ackCallback()(clb)

    case _ =>
  }
}


object Events {
  val callbackPrefix = "events1"
  var events: List[Event] = Nil  // WOWOWOWOW DIRTY ^^

  def getKeyboard(eventIndex: Int): InlineKeyboardMarkup =
    InlineKeyboardMarkup(List((
          if (events.length == 1) Nil
          else if (eventIndex == 0) List(InlineKeyboardButton("next", callbackData = callbackPrefix + "1"))
          else if (eventIndex == events.length - 1) List(InlineKeyboardButton("prev", callbackData = callbackPrefix + (eventIndex-1)))
          else List(InlineKeyboardButton("prev", callbackData = callbackPrefix + (eventIndex-1)),
                    InlineKeyboardButton("next", callbackData = callbackPrefix + (eventIndex+1)))
        ):+ InlineKeyboardButton("close", callbackData = callbackPrefix + "-1")
      )
    )
}

object EventsScraper {
  val browser = JsoupBrowser()
  val baseUrl = "http://memento.epfl.ch/"

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
    doc >?> elementList(".media.event")  match {
      case Some(eventDivs) => eventDivs.map(parseEvent(_)).filter(_.isValidToShow())
      case None => Nil
    }
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
    val periodFmter = new PeriodFormatterBuilder()//.appendDays().appendSuffix(" day", " days")
                                                //.appendSeparator(" and ")
                                                .appendHours().appendSuffix(" hour", " hours")
                                                .appendSeparator(" and ")
                                                .appendMinutes().appendSuffix(" minute", " minutes")
                                                //.appendSeparator(" and ")
                                                //.appendSeconds().appendSuffix(" second", " seconds")
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

    "*"+title + "*\n\n" +
    dateFmter.print(startDate.toLocalDate()) + " _-to-_ " + dateFmter.print(endDate.toLocalDate()) + "\n" +
    "*From*: *" + timeFmter.print(startDate.toLocalTime()) + "* *Until*: *" + timeFmter.print(endDate.toLocalTime()) +
      "* (_" + periodFmter.print(remaining) + " remaining_) " + "\n" +
    ((location, locationUrl) match {
      case (Some(loc), Some(locUrl)) => "*At*: [" + loc + "](" + locUrl + ")\n\n"
      case (Some(loc), None) => "*At*: " + loc + "\n\n"
      case _ => ""
    }) +
    "[export ICal link](" + exportUrl + ")\n" +
    "[description](" + url +")"
  }
  // override def toString() = eventDiv.toString() HTML ?...
  // TODO beautiful formatting....what does telegram allow to do ? print image of event ? why not just render the whole <div> if allowed
}
