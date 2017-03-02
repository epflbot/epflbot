package ch.epfl.telegram

import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.models._
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.methods.{EditMessageText, ParseMode}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element

/**
  * Add Menus (EPFL Menus) useful commands.
  */
trait Menus extends Commands with Callbacks { _ : TelegramBot =>

  import Menus.callbackPrefix

  /**
    * Crude command to get today EPFL events. (that are not "already finished")
    */
  on("/menus") { implicit msg => _ =>
    Menus.menus =  MenusScraper.retrieveMenus()  // TODO DIRTY !!!!!!! ^^
    Menus.menus match {
      case event::_ => reply(
        event.toString(),
        replyMarkup = Menus.getKeyboard(0),
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
            text = Menus.menus(index).toString,
            replyMarkup = Menus.getKeyboard(index),
            parseMode = ParseMode.Markdown
          )
        )
      }
      ackCallback()(clb)
    case _ =>
  }
}

object Menus {
  val callbackPrefix = "menus1"
  var menus: List[Menu] = Nil  // WOWOWOWOW DIRTY ^^

  def getKeyboard(menuIndex: Int): InlineKeyboardMarkup =
    InlineKeyboardMarkup(List((
      if (menus.length == 1) Nil
      else if (menuIndex == 0) List(InlineKeyboardButton("next", callbackData = callbackPrefix + "1"))
      else if (menuIndex == menus.length - 1) List(InlineKeyboardButton("prev", callbackData = callbackPrefix + (menuIndex-1)))
      else List(InlineKeyboardButton("prev", callbackData = callbackPrefix + (menuIndex-1)),
        InlineKeyboardButton("next", callbackData = callbackPrefix + (menuIndex+1)))
      ):+ InlineKeyboardButton("close", callbackData = callbackPrefix + "-1")
    )
    )
}

// TODO share similar code between menus and events => trait scrapper
object  MenusScraper {
  val browser = JsoupBrowser()
  val baseUrl = "https://menus.epfl.ch/cgi-bin/getMenus"

  private def parseMenu(menuDiv: Element): Menu = {
    val description = (menuDiv >> element(".desc")).text
    val resto = (menuDiv >> element(".resto")).text
    val prix = {
      val p = (menuDiv >> element(".prix")).text.split(" ")
      (menuDiv >> elementList(".prix span")) map { elem =>
        (elem.text, p(p.indexOf(elem.text)+1))
      }
    }
    Menu(description, resto, prix, menuDiv)
  }

  def retrieveMenus(): List[Menu] = {
    val doc = browser.get(baseUrl)
    doc >?> elementList("#menulist li")  match {
      case Some(menusDivs) => {
        menusDivs.map(parseMenu(_)).filter(_.isValidToShow())
      }
      case None => Nil
    }
  }
}

case class Menu(description: String,
                resto: String,
                prix: List[(String, String)],
                eventDiv: Element)  // just in case
{
  def isValidToShow() = true

  override def toString() = {
    "*" + description + "*\n\n" +
    resto + "\n\n" +
    (prix map { case (cat, prix) =>
      (if(cat.isEmpty) " "*9 else  "*" + cat + "* - ") + prix + " CHF\n"
    }).mkString("")
  }
}