package ch.epfl.telegram

import akka.http.scaladsl.util.FastFuture
import com.sksamuel.elastic4s.ElasticDsl._
import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api.{Commands, TelegramBot}
import info.mukel.telegrambot4s.methods.{EditMessageText, ParseMode}
import info.mukel.telegrambot4s.models.{InlineKeyboardMarkup, _}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Add survey-like commands.
  * /survey
  * /feedback
  */
trait Survey extends Commands { _: TelegramBot =>

  import Survey._

  private def editMessage(message: Message, text: String, replyMarkup: Option[ReplyMarkup] = None): Future[_] =
    request(
      EditMessageText(
        chatId = message.chat.id,
        messageId = message.messageId,
        text = text,
        replyMarkup = replyMarkup,
        parseMode = ParseMode.Markdown
      )
    )

  override def onCallbackQuery(cb: CallbackQuery): Unit = {
    cb match {
      case CallbackQuery(_, user, Some(message), _, _, Some(data), _) =>

        logger.debug("callback query data {}", data)
        val Array(questionIdx, answerIdx) = data.split(":").map(_.toInt)
        val (question, answer) =
          if (questionIdx < generalQuestions.size) {
            val (question, answers) = generalQuestions(questionIdx)
            question -> answers(answerIdx)
          } else {
            val question = servicesQuestions(questionIdx - generalQuestions.size)
            question -> answerIdx.toString
          }

        for {
          _ <- putAnswer(user.id, Map(question -> answer))
          next <- nextQuestion(user.id, Some(questionIdx + 1))
        } {
          next match {
            case Some((text, markup)) =>
              logger.info("user {} question {} markup {}", user.id.toString, question, markup.toString)
              editMessage(message, text, markup)
            case None =>
              editMessage(message, conclusion)
          }
        }

      case _ =>
        super.onCallbackQuery(cb)
    }
  }

  on("/survey") { implicit msg => _ =>
    for {
      user <- msg.from
      next <- nextQuestion(user.id)
    } {
      next match {
        case Some((text, markup)) =>
          logger.info("user {} new survey", user.id)
          reply(introduction, parseMode = ParseMode.Markdown)
          reply(text, replyMarkup = markup, parseMode = ParseMode.Markdown)
        case None =>
          logger.info("user {} completed survey", user.id)
          reply(conclusion, parseMode = ParseMode.Markdown)
      }
    }
  }

  on("/feedback") { implicit msg => args =>
    for {
      user <- msg.from
      _ <- putFeedback(user.id, args.mkString(" "))
    } {
      reply("Thanks for the comment!")
    }
  }

}



object Survey {

  // curl http://localhost:9200/indexName -X PUT
  val surveyStore = "survey" / "welcome"
  val feedbackStore = "feedback" / "welcome"

  def putAnswer(userId: Long, responses: Map[String, String])(implicit ec: ExecutionContext): Future[Unit] =
    ElasticSearch {
      update(userId) in surveyStore docAsUpsert responses
    } map(_ => ())

  def getAnswers(userId: Long)(implicit ec: ExecutionContext): Future[Map[String, String]] = {
    ElasticSearch {
      get(userId) from surveyStore
    } map(_.sourceAsMap.mapValues(_.toString))
  }

  def putFeedback(userId: Long, feedback: String)(implicit ec: ExecutionContext): Future[Unit] =
    ElasticSearch {
      indexInto(feedbackStore) fields("user" -> userId, "feedback" -> feedback)
    }  map (_ => ())

  def nextQuestion(userId: Long, nextQuestionNumber: Option[Int] = None)(implicit ec: ExecutionContext)
  : Future[Option[(String, InlineKeyboardMarkup)]] = {

    require(nextQuestionNumber.forall(_ >= 0), "next question number cannot have negative index")
    nextQuestionNumber match {

      case Some(step) if step < generalQuestions.size =>
        val (question, answers) = generalQuestions(step)
        val buttons = answers.zipWithIndex.map { case (answer, i) =>
          InlineKeyboardButton(answer, callbackData = s"$step:$i")
        }.toList
        val next = s"*$question*" -> InlineKeyboardMarkup(buttons.grouped(2).toList)
        FastFuture.successful(next)

      case Some(step) if step < generalQuestions.size + servicesQuestions.size =>
        val service = servicesQuestions(step - generalQuestions.size)
        val question = s"*How likely would you use the following service through EPFLBot?*\n\u27A1 _${service}_"
        val buttons = (1 to 3).map { i =>
          val answer = List.fill(i)("\u2B50").mkString
          InlineKeyboardButton(answer, callbackData = s"$step:$i")
        }
        val next = question -> InlineKeyboardMarkup(List(buttons))
        FastFuture.successful(next)

      case Some(step) =>
        FastFuture.successful(None)

      case None =>
        val res = getAnswers(userId).flatMap { responses =>
          println(responses)
          nextQuestion(userId, Some(responses.size))
        }
        res.onComplete(println)
        res
    }
  }

  val introduction = "This is a quick survey to improve your EPFLBot experience :D.\n" +
    "Answers are confidential and won't be publicly disclosed."

  val generalQuestions = List(
    "What is your current position at EPFL?" -> List(
      "student",
      "phd candidate",
      "employee",
      "professor",
      "visitor",
      "other"
    ),
    "What is your current degree?" -> List(
      "1st year bachelor",
      "2nd year bachelor",
      "3rd year bachelor",
      "1st year master",
      "2nd year master",
      "I'm not a student"
    ),
    "What is your school?" -> List(
      "ENAC",
      "SB",
      "IC",
      "SV",
      "STI",
      "CDM",
      "CDH",
      "I'm not part of a school"
    ),
    "How old are you?" -> List(
      "< 18",
      "18-25",
      "26-30",
      "31-40",
      "41-50",
      "> 50"
    ),
    "Usually how do you travel to/from EPFL?" -> List(
      "M2",
      "M2 + train",
      "M2 + bus",
      "only bus",
      "bike",
      "car",
      "feet",
      "otherwise"
    ),
    "Usually how do you eat at EPFL?" -> List(
      "cafeterias",
      "restaurants",
      "food trucks",
      "tupperware",
      "sandwiches",
      "otherwise"
    )
  )

  val servicesQuestions = List(
    "room booking",
    "people search",
    "next public transports",
    "restaurant menu",
    "campus map search",
    "today's events",
    "today's timetable",
    "today's sport classes",
    "moodle access",
    "EPFLMeme",
    "satellite",
    "AGEPOLY services",
    "EPFL daily sum up",
    "subscribe to specific information channels",
    "meeting new people"
  )

  val conclusion = "You completed the whole survey \\o/. Thank you for your contributions!\n" +
    "You can add your own ideas using:\n" +
    "  _/feedback I'd like to see daily menu reminder!_"

}
