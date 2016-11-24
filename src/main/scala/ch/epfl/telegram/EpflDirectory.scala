package ch.epfl.telegram

import java.net.URLEncoder

import scala.util.Try
//import EpflBot
import net.ruippeixotog.scalascraper.model.Document
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api.{Commands, TelegramBot}
import info.mukel.telegrambot4s.methods.{AnswerCallbackQuery, AnswerInlineQuery}
import info.mukel.telegrambot4s.models._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

import scala.util.{Failure, Success}

/**
  * Created by aimee on 11/22/16.
  */
trait EpflDirectory extends TelegramBot {

  override def handleInlineQuery(inlineQuery: InlineQuery) : Unit = {

    val persons = Try(DirectoryScraper.getLines(inlineQuery.query)).getOrElse(Seq.empty)

    var cnt = 0
    val results : Seq[InlineQueryResult] = for(person <- persons) yield {
      cnt += 1
      InlineQueryResultArticle(cnt.toString, person.name,
                              InputTextMessageContent("Hello " + person.name),
                              thumbUrl = person.photo,
                              url = person.url,
                              description = person.unit)

    }
    api.request(AnswerInlineQuery(inlineQuery.id, results))
      .onComplete {
        case Success(_) => println("OK")
        case Failure(e) => println(e)
      }
  }
}

case class Person(name: String, mail: String, url : String, function: String, unit : String, photo : Option[String] )

object DirectoryScraper {
  val browser = new JsoupBrowser
  val base_url = "https://search.epfl.ch/psearch.action?q="

  def getLines(query : String) : Seq[Person] = {
    val doc = browser.get(base_url + URLEncoder.encode(query, "UTF-8"))
    print("Title : " + doc.title + "\n********************\n")
    //getPerson(doc)
    val option_list = doc >?> element("#search-results > ol")

    option_list match {
      case Some(list) => {
        val values = list >> elements("> li")

        values.par.map { v  =>
          val url = v >> attr("href")("a")
          val personEntry = browser.get(url + URLEncoder.encode(query, "UTF-8"))
          getPerson(personEntry)
        }.toList

      }
      case None => {
        doc >?> element("#main-navigation > ul > li > a") match {
          case Some(x) =>
            Array(getPerson(doc)).toSeq
          case None =>
            Seq.empty
        }
      }
    }
  }

  def getPerson(personEntry : Document) : Person = {
    val url = personEntry >> element("#breadcrumbs > li.last") >> attr("href")("a")
    val function_info = personEntry >> element("#content > div > div")

    val function = function_info >> element("div.topaccredlarge") >> text("a")
    val unit = function_info >> element("div.topaccred") >> text("a")

    val personal_info = personEntry >> element("#main-content > div.right-col > div.box")
    val photo_url = personal_info >> element("div.portrait") >?> attr("src")("img")

    val name = personal_info >> element("div.presentation") >> text("h4")
    val mail = ""//personal_info >> element("div.presentation") >> optionFirst(text("a"))
    print(name+"\n")
    Person(name, mail, url, function, unit, photo_url)
  }
}
