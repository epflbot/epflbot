package ch.epfl.telegram.models

import com.sksamuel.elastic4s.ElasticDsl._
import io.circe.generic.JsonCodec
import com.sksamuel.elastic4s.circe._
import org.elasticsearch.action.DocWriteResponse.Result

import scala.concurrent.{ExecutionContext, Future}

@JsonCodec case class EPFLUser(id: Option[Int],
                               sciper: Int,
                               firstName: String,
                               name: String,
                               displayName : String,
                               employeeType : Option[String],
                               email: String,
                               gaspar: String,
                               where: String)

object EPFLUser {

  val userIndex = "user" / "tequila"

  def putUser(user: EPFLUser)(implicit ec: ExecutionContext): Future[Unit] =
    es.execute {
      update(user.id) in userIndex docAsUpsert user
    } map (_ => ())

  def putUserBySciper(users: Seq[EPFLUser])(implicit ec: ExecutionContext): Future[Unit] =
    es.execute {
      bulk(
        for (user <- users)
          yield  update(user.sciper) in userIndex docAsUpsert user
      )
    } map (_ => ())

  def fromId(id: Int)(implicit ec: ExecutionContext): Future[Option[EPFLUser]] =
    es.execute {
      get(id) from userIndex
    } map {
      _.to[Option[EPFLUser]]
    }

  def removeId(id: Int)(implicit ec: ExecutionContext): Future[Boolean] =
    es.execute {
      delete(id) from userIndex
    }.map(_.status() == Result.DELETED)

}
