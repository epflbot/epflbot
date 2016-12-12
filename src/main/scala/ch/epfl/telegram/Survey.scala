package ch.epfl.telegram

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import info.mukel.telegrambot4s.api.{Commands, TelegramBot}
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.models.{KeyboardButton, Message, ReplyKeyboardMarkup, ReplyMarkup}
import org.elasticsearch.common.settings.Settings
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

/**
  * Add survey-like commands.
  * /survey
  */
trait Survey extends Commands { _: TelegramBot =>

  import Survey._

  on("/survey") { implicit msg => _ =>
    for {
      user <- msg.from
      responses <- getState(user.id)
    } {
      questions.find(q => !responses.contains(q._1)) match {
        case Some((nextQuestion, answers)) =>
          val buttons = answers.map(KeyboardButton(_)).grouped(2).toList
          reply(nextQuestion, replyMarkup = ReplyKeyboardMarkup(buttons, oneTimeKeyboard = Some(true)))
        case None =>
          reply("Thanks, this is survey is completed!")
      }
    }
  }
}

object Survey {

  val callbackPrefix = "survey"

  type Responses = Map[String, String]

  private val es = {
    val settings = Settings.builder().put("cluster.name", "epflbot").build()
    val uri = "elasticsearch://localhost:9300"
    println(DateTime.now)
    val client = ElasticClient.transport(settings, ElasticsearchClientUri(uri))
    println(DateTime.now)
    client
  }

  val index = "survey" / "welcome"

  def putState(userId: Long, responses: Responses)(implicit ec: ExecutionContext): Future[Unit] =
    es.execute(indexInto(index) id userId fields responses).map(_ => ())

  def getState(userId: Long)(implicit ec: ExecutionContext): Future[Responses] =
    es.execute(get(userId) from index).map(_.sourceAsMap.mapValues(_.toString))

  val questions = List(
    "What is your current status?" -> List("student",
      "employee",
      "professor",
      "other"),
    "How old are you" -> List("-18", "18-25", "26-30", "+30")
  )

}
