package ch.epfl.telegram.commands

import java.net.URLEncoder

import scala.util.Try
//import EpflBot
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api.TelegramBot
import info.mukel.telegrambot4s.methods.AnswerInlineQuery
import info.mukel.telegrambot4s.models._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Document

import scala.util.{Failure, Success}

/**
  * Created by aimee on 11/22/16.
  */
trait InlineEpflDirectory extends TelegramBot {

  override def onInlineQuery(inlineQuery: InlineQuery) : Unit = {

    println(s"Query: ${inlineQuery.query}")
    val t = Try(DirectoryScraper.search(inlineQuery.query))
    println(t)
    val persons = t.getOrElse(Seq.empty)

    var cnt = 0
    val results : Seq[InlineQueryResult] = for(person <- persons) yield {
      cnt += 1
      InlineQueryResultArticle(cnt.toString, person.name,
                              InputTextMessageContent(person.name + " " + person.function + " in " + person.unit +
                                                      " mail: " + person.mail),
                              thumbUrl = person.photo,
                              url = person.url,
                              description = person.unit)

    }
    println(results)
    request(AnswerInlineQuery(inlineQuery.id, results))
      .onComplete {
        case Success(_) => println("OK")
        case Failure(e) => println(e)
      }
  }
}

case class Person(name: String, mail: String, url : String, function: String, unit : String, photo : Option[String] )

trait EpflDirectory {
  def search (query : String) : Seq[Person]
}

object DirectoryScraper extends EpflDirectory {
  val browser = new JsoupBrowser
  //TODO For now only 10 result, the ideal is to load results when scrolling
  val base_url = "https://search.epfl.ch/psearch.action?q="

  def search(query : String) : Seq[Person] = {
    val doc = browser.get(base_url + URLEncoder.encode(query, "UTF-8"))
    val option_list = doc >?> element("#search-results > ol")

    option_list match {
      case Some(list) => {
        val values = list >> elements("> li")

        values.par.flatMap { v =>
          Try({
            val url = v >> attr("href")("a")
            val personEntry = browser.get(url + URLEncoder.encode(query, "UTF-8"))
            getPerson(personEntry)
          }).toOption
        }.toList

      }
      case None => {
        doc >?> element("#main-navigation > ul > li > a") match {
          case Some(_) =>
            Array(getPerson(doc)).toSeq
          case None =>
            Seq.empty
        }
      }
    }
  }

  def getPerson(personEntry : Document) : Person = {
    val url = personEntry >> element("#breadcrumbs > li.last") >> attr("href")("a")
    val function_info = personEntry >> element("#content > div > div > div")

    val function = function_info >> element("div.topaccredlarge") >> text("a")
    val unit = function_info >> element("div.topaccred") >> text("a")

    val personal_info = personEntry >> element("#main-content > div.right-col > div.box")
    val photo_url = personal_info >> element("div.portrait") >?> attr("src")("img")

    val name = personal_info >> element("div.presentation") >> text("h4")
    val mail_script = (personal_info >> element("div.presentation > script")).innerHtml

    val mail_extractor = """msgto\('(\S*)','(\S*)'\)""".r

    val mail = mail_script match {
      case mail_extractor(name, domain) => s"$name@$domain"
      case _ => "None"
    }
    Person(name, mail, url, function, unit, photo_url)
  }
}
