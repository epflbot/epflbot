package ch.epfl.telegram

import com.sksamuel.elastic4s.{ElasticsearchClientUri, TcpClient}
import com.sksamuel.elastic4s.ElasticDsl._
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

}
