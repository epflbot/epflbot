package ch.epfl.telegram

case class TequilaConfig(host: String, service: String, request: String, require: String, allows: String)
case class HttpConfig(interface: String, port: Int)

case class EPFLBotConfig(tequila: TequilaConfig, http: HttpConfig)
