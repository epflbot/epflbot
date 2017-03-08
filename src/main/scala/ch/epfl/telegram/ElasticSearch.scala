package ch.epfl.telegram

import com.sksamuel.elastic4s.{ElasticsearchClientUri, Executable, TcpClient}

import scala.concurrent.Future
import scala.util.Properties

object ElasticSearch {

  private val es = {
    val interface = Properties.envOrElse("ELASTICSEARCH_INTERFACE", "0.0.0.0")
    TcpClient.transport(
      ElasticsearchClientUri(s"elasticsearch://$interface:9300?cluster.name=epflbot")
    )
  }

  def apply[T, R, Q](t: T)(implicit executable: Executable[T, R, Q]): Future[Q] = es.execute(t)

}
