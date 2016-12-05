package ch.epfl.telegram

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri, Executable}
import org.elasticsearch.common.settings.Settings

import scala.concurrent.Future

object ElasticSearch {

  private val es = {
    val settings = Settings.builder().put("cluster.name", "epflbot").build()
    val uri      = "elasticsearch://localhost:9300"
    val client   = ElasticClient.transport(settings, ElasticsearchClientUri(uri))
    client
  }

  def apply[T, R, Q](t: T)(implicit executable: Executable[T, R, Q]): Future[Q] = es.execute(t)

}
