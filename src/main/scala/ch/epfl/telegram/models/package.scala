package ch.epfl.telegram

import com.sksamuel.elastic4s.{ElasticsearchClientUri, TcpClient}

package object models {

  private[models] val es = {
    import Config.elasticsearch.{clusterName, host, port}
    TcpClient.transport(
      ElasticsearchClientUri(s"elasticsearch://$host:$port?cluster.name=$clusterName")
    )
  }

}
