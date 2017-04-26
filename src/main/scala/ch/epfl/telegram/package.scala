package ch.epfl

import java.util.logging.LogManager

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

package object telegram {

  LogManager.getLogManager.readConfiguration()
  lazy val version = BuildInfo.version // sbt build info created at compiled time

  val Config = ConfigFactory.load().as[EPFLBotConfig]("epflbot")

}
