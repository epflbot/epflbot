package ch.epfl.telegram

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticsearchClientUri, TcpClient}

import scala.concurrent.ExecutionContext.Implicits.global

package object models {

  private[models] val es = {
    import Config.elasticsearch.{clusterName, host, port}
    TcpClient.transport(
      ElasticsearchClientUri(s"elasticsearch://$host:$port?cluster.name=$clusterName")
    )
  }

  List(Reaction.feedbackIndex, Reaction.surveyIndex, EPFLUser.epflUserIndex)
    .foreach { indexType =>
      println(indexType)
      es.execute {
          createIndex(indexType.index)
        }
        .map(println)
    }

  import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
  import io.circe.jawn.decode
  import io.circe.{Decoder, Encoder, Json, Printer}

  import scala.annotation.implicitNotFound

  object dropNulls {
    @implicitNotFound(
      "No Decoder for type ${T} found. Use 'import io.circe.generic.auto._' or provide an implicit Decoder instance ")
    implicit def hitReaderWithCirce[T](implicit decoder: Decoder[T]): HitReader[T] = new HitReader[T] {
      override def read(hit: Hit): Either[Throwable, T] = decode[T](hit.sourceAsString)
    }

    @implicitNotFound(
      "No Encoder for type ${T} found. Use 'import io.circe.generic.auto._' or provide an implicit Encoder instance ")
    implicit def indexableWithCirce[T](
        implicit encoder: Encoder[T],
        printer: Json => String = Printer.noSpaces.copy(dropNullKeys = true).pretty): Indexable[T] = new Indexable[T] {
      override def json(t: T): String = printer(encoder(t))
    }
  }

}
