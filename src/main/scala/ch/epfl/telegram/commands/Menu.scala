package ch.epfl.telegram.commands

import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.methods.{EditMessageText, ParseMode}
import info.mukel.telegrambot4s.models._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element

/**
  * Add Menus (EPFL Menus) useful commands.
  */
trait Menus extends Commands with Callbacks { _ : TelegramBot =>

  import Restos.callbackPrefix

  /**
    * Crude command to get today EPFL menus. (that are not "already finished")
    */
  on("/menus", "today's menus") { implicit msg => args =>

    val restos = MenusScraper.retrieveMenus().groupBy(_.resto).filterKeys {
      def buildPredicate(key: String, args: Seq[String]): Boolean = args match {
        case Seq(resto, tail@_*) => key.toLowerCase.contains(resto.toLowerCase) ||
                                    resto.toLowerCase.contains(key.toLowerCase) ||
                                    buildPredicate(key, tail)
        case _ => false
      }
      key => if (args.isEmpty) true else buildPredicate(key, args)
    } map {
      case (name, menus) => Resto(name, menus)
    }

    Restos.setCached(restos.toList)
    Restos.cached.get match {
      case menu::_ => reply(
        menu.toString(),
        replyMarkup = Restos.getKeyboard(0),
        parseMode = ParseMode.Markdown
      )
      case _ => reply("nothing to show")
    }

  }

  onCallbackWithTag(callbackPrefix) {
    case clb @ CallbackQuery(_, _, Some(message), _, _, Some(data), _) =>
      logger.debug("callback query data {}", data)

      data.toInt match {
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
            text = Restos.cached.get(index).toString,
            replyMarkup = Restos.getKeyboard(index),
            parseMode = ParseMode.Markdown
          )
        )
      }
      ackCallback()(clb)
    case _ =>
  }
}

object Restos extends Cachable[List[Resto]] {
  val callbackPrefix = "menus1"

  def getKeyboard(restoIndex: Int): InlineKeyboardMarkup =
    InlineKeyboardMarkup(List((
      if (cached.get.length == 1) Nil
      else if (restoIndex == 0) List(InlineKeyboardButton("next", callbackData = callbackPrefix + "1"))
      else if (restoIndex == cached.get.length - 1) List(InlineKeyboardButton("prev", callbackData = callbackPrefix + (restoIndex-1)))
      else List(InlineKeyboardButton("prev", callbackData = callbackPrefix + (restoIndex-1)),
        InlineKeyboardButton("next", callbackData = callbackPrefix + (restoIndex+1)))
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

  override def toString(): String = {
    "_" + description + "_\n" +
    (prix map { case (cat, prix) =>
      (if(cat.isEmpty) " "*4 else  "*" + cat + "* - ") + prix + " CHF"
    }).mkString(", ")
  }
}

case class Resto(name: String, menus: List[Menu])
{
  override def toString: String = {
    "*" + name + "*" + "\n" +
    (menus map { _.toString() }).mkString("\n\n")
  }
}