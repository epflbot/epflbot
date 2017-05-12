package ch.epfl.telegram

case class ElasticsearchConfig(host: String, port: Int, clusterName: String)
case class TequilaConfig(host: String, service: String, request: String, require: String, allows: String, tequilaRedirectPort: Int)
case class HttpConfig(interface: String, port: Int)

case class EPFLBotConfig(elasticsearch: ElasticsearchConfig, tequila: TequilaConfig, http: HttpConfig)
