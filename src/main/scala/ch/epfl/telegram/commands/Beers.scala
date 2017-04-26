package ch.epfl.telegram.commands

import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.methods.{EditMessageText, ParseMode}
import info.mukel.telegrambot4s.models._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.{Document, Element}

/**
  * Add Satellite useful commands (beer...).
  */
trait Beers extends Commands with Callbacks { _ : TelegramBot =>

  import Sat.callbackPrefix

  /**
    * Crude command to get beers !.
    */
  on("/beers", "beers ... what else ") { implicit msg => _ =>

    Sat.setCached(SatScraper.parseSat())
    Sat.cached match {
      case Some(sat) => reply(
        sat.toString(),
        replyMarkup = Sat.getMenuKeyboard(),
        parseMode = ParseMode.Markdown
      )
      case None => reply("nothing to show")
    }
  }

  onCallbackWithTag(callbackPrefix) {
    case clb@CallbackQuery(_, _, Some(message), _, _, Some(data), _) =>
      logger.debug("callback query data {}", data)

      data match {
        case "close" => request (
          EditMessageText (
            messageId = message.messageId,
            chatId = message.chat.id,
            text = message.text.getOrElse("nothing to show"),
            parseMode = ParseMode.Markdown
          )
        )
        case "back" => request(
          EditMessageText(messageId = message.messageId,
            chatId = message.chat.id,
            text = Sat.cached.get.toString(),
            replyMarkup = Sat.getMenuKeyboard(),
            parseMode = ParseMode.Markdown
          )
        )
        case cat => request(
          EditMessageText(messageId = message.messageId,
            chatId = message.chat.id,
            text = Sat.cached.get.items(cat).map { _.toString() }.mkString("\n"),
            replyMarkup = InlineKeyboardMarkup(Seq(Seq(InlineKeyboardButton("back", callbackData = callbackPrefix + "back")))),
            parseMode = ParseMode.Markdown
          )
        )
      }
      ackCallback()(clb)
    case _ =>
  }
}

object Sat extends Cachable[Satellite] {
  val callbackPrefix = "satellite1"

  def getMenuKeyboard(): InlineKeyboardMarkup =
    InlineKeyboardMarkup((cached.get.items.keys.map { cat =>
        InlineKeyboardButton(cat, callbackData = callbackPrefix + cat)
    }.toSeq :+ InlineKeyboardButton("close", callbackData = callbackPrefix + "close")).grouped(2).toSeq)
}

object  SatScraper {
  val browser = JsoupBrowser()
  val baseUrl = "https://satellite.bar/bar/biere.php"
  val categories = Map("pression" -> "drafts",
                        "mois_pression" -> "draft(s) of the month",
                        "mois_bouteille" -> "bottled beer(s) of the month",
                        "grande_bouteille" -> "big bottles") //, "bouteille" -> "bottles")
  // FIXME paginate long messages ex: bouteilles is even too long to be embeded in a single message => too long exception

  private def parseBeer(beerLink: Element): Beer = {
    implicit val doc = browser.get(baseUrl+beerLink.attr("href"))

    def retrieveText(cssSelector: String)(implicit doc: Document): Option[String] =
      doc >?> element(cssSelector) map (elem => elem.text)

    val name = retrieveText("#ALE > h2:nth-child(1) > span:nth-child(1)")
    val brasserie = retrieveText("#ALE > div:nth-child(2) > table:nth-child(4) > tbody:nth-child(1) > tr:nth-child(2) > td:nth-child(2)")
    val typ = retrieveText("#ALE > div:nth-child(2) > table:nth-child(4) > tbody:nth-child(1) > tr:nth-child(2) > td:nth-child(4)")
    val prix = retrieveText("#ALE > div:nth-child(2) > table:nth-child(4) > tbody:nth-child(1) > tr:nth-child(4) > td:nth-child(4)")
    val contenu = retrieveText("#ALE > div:nth-child(2) > table:nth-child(4) > tbody:nth-child(1) > tr:nth-child(3) > td:nth-child(4)")
    val pays = retrieveText("#ALE > div:nth-child(2) > table:nth-child(4) > tbody:nth-child(1) > tr:nth-child(3) > td:nth-child(2)")
    val description = retrieveText("#ALE > div:nth-child(2) > table:nth-child(4) > tbody:nth-child(1) > tr:nth-child(5) > td:nth-child(1)")

    Beer(name, brasserie, typ, prix, contenu, pays, description)
  }

  def parseSat(): Satellite = {
    val items = categories.map { case (cat, name) =>
      val doc = browser.get(baseUrl+"?type_biere="+cat)
      val beers = doc >?> elementList("p.center ~ p a.int_link")  match {
        case Some(beerLinks) => {
          beerLinks.map(parseBeer(_)).filter(_.isValidToShow())
        }
        case None => Nil
      }
      name -> beers
    }

    Satellite(items)
  }
}

case class Beer(name: Option[String],
                brasserie: Option[String],
                typ: Option[String],
                prix: Option[String],
                contenu: Option[String],  // TODO: uniformize/parse better (cl, dl, nothing..)
                pays: Option[String],
                description: Option[String])
{
  def isValidToShow() = !name.isEmpty

  override def toString(): String = {
    "̣*" + name.get + "*\n" +
    brasserie.map("_brasserie_: " + _ + "\n").getOrElse("") +
    typ.map("_type_: " + _ + "\n").getOrElse("") +
    prix.map("_prix_: " + _ + contenu.map(" (" + _ + ")").getOrElse("") + "\n").getOrElse("") +
    pays.map("_pays_: " + _ + "\n").getOrElse("") +
    description.map(_ + "\n").getOrElse("")
  }
}

case class Satellite(items: Map[String, List[Beer]])
{
  override def toString(): String = {
    "beers @ sat !!\n"
  }
}