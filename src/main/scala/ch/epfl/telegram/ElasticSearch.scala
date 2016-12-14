package ch.epfl.telegram

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri, Executable}
import org.elasticsearch.common.settings.Settings

import scala.concurrent.Future
import scala.util.Properties

object ElasticSearch {

  private val es = {
    val interface = Properties.envOrElse("ELASTICSEARCH_INTERFACE", "0.0.0.0")
    val settings = Settings.builder().put("cluster.name", "epflbot").build()
    val uri      = s"elasticsearch://$interface:9300"
    val client   = ElasticClient.transport(settings, ElasticsearchClientUri(uri))
    client
  }

  def apply[T, R, Q](t: T)(implicit executable: Executable[T, R, Q]): Future[Q] = es.execute(t)

}
