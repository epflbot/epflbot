package ch.epfl.telegram.models

import com.sksamuel.elastic4s.ElasticDsl._

import scala.concurrent.{ExecutionContext, Future}

object Reaction {

  val surveyStore   = "survey" / "welcome"
  val feedbackStore = "feedback" / "welcome"

  def putAnswer(userId: Long, responses: Map[String, String])(implicit ec: ExecutionContext): Future[Unit] =
    es.execute {
      update(userId) in surveyStore docAsUpsert responses
    } map (_ => ())

  def getAnswers(userId: Long)(implicit ec: ExecutionContext): Future[Map[String, String]] = {
    es.execute {
      get(userId) from surveyStore
    } map (_.sourceAsMap.mapValues(_.toString))
  }

  def putFeedback(userId: Long, feedback: String)(implicit ec: ExecutionContext): Future[Unit] =
    es.execute {
      indexInto(feedbackStore) fields ("user" -> userId, "feedback" -> feedback)
    } map (_ => ())

}
