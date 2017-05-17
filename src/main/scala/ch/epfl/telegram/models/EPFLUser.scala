package ch.epfl.telegram.models

import com.sksamuel.elastic4s.ElasticDsl._
import io.circe.generic.JsonCodec
import com.sksamuel.elastic4s.circe._
import org.elasticsearch.action.DocWriteResponse.Result

import scala.concurrent.{ExecutionContext, Future}

@JsonCodec case class TelegramInfo(id: Int, username: Option[String])

@JsonCodec case class EPFLUser(id: Int,
                               telegramInfo: Option[TelegramInfo],
                               firstName: String,
                               name: String,
                               displayName: String,
                               employeeType: Option[String],
                               email: String,
                               gaspar: String,
                               where: String) {

  def sciper: Int = id

  def isAvailableOnTelegram: Boolean =
    telegramInfo
      .map(_.username.isDefined)
      .getOrElse(false)
}

/**
  * Put* operations will completely wipe existing data (same effect as delete and then insert)
  * Update* operations will refresh existing data (upsert).
  */
object EPFLUser {

  val epflUserIndex = "user" / "epfl"

  def putUser(user: EPFLUser)(implicit ec: ExecutionContext): Future[Unit] = {

    es.execute {
      update(user.id) in epflUserIndex docAsUpsert user
    } map (_ => ())
  }

  def updateUser(user: EPFLUser)(implicit ec: ExecutionContext): Future[Unit] = {
    import ch.epfl.telegram.models.dropNulls._
    es.execute {
      update(user.id) in epflUserIndex docAsUpsert user
    } map (_ => ())
  }

  def putUserSeq(users: Seq[EPFLUser])(implicit ec: ExecutionContext): Future[Unit] = {
    es.execute {
      bulk(
        for (user <- users)
          yield update(user.sciper) in epflUserIndex docAsUpsert user
      )
    } map (_ => ())
  }

  def updateUserSeq(users: Seq[EPFLUser])(implicit ec: ExecutionContext): Future[Unit] = {
    import ch.epfl.telegram.models.dropNulls._
    es.execute {
      bulk(
        for (user <- users)
          yield update(user.sciper) in epflUserIndex docAsUpsert user
      )
    } map (_ => ())
  }

  def fromId(id: Int)(implicit ec: ExecutionContext): Future[Option[EPFLUser]] =
    es.execute {
      get(id) from epflUserIndex
    } map {
      _.to[Option[EPFLUser]]
    }

  def removeId(id: Int)(implicit ec: ExecutionContext): Future[Boolean] =
    es.execute {
        delete(id) from epflUserIndex
      }
      .map(_.status() == Result.DELETED)

  def searchDirectory(userQuery: String)(implicit ec: ExecutionContext): Future[Seq[EPFLUser]] =
    es.execute {
      // Limit the number of results to avoid getting the whole EPFL directory on a single query.
      search(epflUserIndex) query userQuery limit 20
    } map {
      _.to[EPFLUser]
    }

  def fromTelegramId(id: Int)(implicit ec: ExecutionContext): Future[Seq[EPFLUser]] =
    es.execute {
      search(epflUserIndex) query termQuery("telegramInfo.id" -> id)
    } map {
      _.to[EPFLUser]
    }

  def unlinkTelegramId(id: Int)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      epflUsers <- fromTelegramId(id)
      u         <- putUserSeq(epflUsers.map(_.copy(telegramInfo = None)))
    } yield u
  }
}
