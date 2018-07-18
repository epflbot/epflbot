package ch.epfl.telegram.external

import com.typesafe.scalalogging.Logger
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.syntax._
import io.socket.{IOAcknowledge, IOCallback, SocketIO, SocketIOException}
import org.json.JSONObject

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

class ScalaSocketIO(socketURI: String) { self =>

  private val logger = Logger("socket.io")

  private val socket = {
    val instance = new SocketIO(socketURI)
    instance.connect(new IOCallback() {
      override def onConnect(): Unit                                          = self.onConnect()
      override def onDisconnect(): Unit                                       = self.onDisconnect()
      override def onError(socketIOException: SocketIOException): Unit        = self.onError(socketIOException)
      override def on(event: String, ack: IOAcknowledge, args: AnyRef*): Unit = self.on(event, ack, args)
      override def onMessage(event: String, ack: IOAcknowledge): Unit         = self.onMessage(event, ack)
      def onMessage(json: JSONObject, ack: IOAcknowledge): Unit               = self.onMessage(json, ack)
    })
    instance
  }

  def onConnect(): Unit = {
    logger.info("SocketIO connected")
  }

  def onDisconnect(): Unit = {
    logger.info("SocketIO disconnected")
  }

  def onError(socketIOException: SocketIOException): Unit = {
    logger.error("SocketIO error", socketIOException)
  }

  /*
  should change to once we figured out what is the difference and behaviour the three following functions.
  def on: PartialFunction[String, Unit]
   */

  def on(event: String, ack: IOAcknowledge, args: AnyRef*): Unit = {
    logger.debug("SocketIO event {}", event)
    // response should ack using json
  }

  def onMessage(message: String, ack: IOAcknowledge): Unit = {
    logger.debug("SocketIO message {}", message)
    // response should ack using json
  }

  def onMessage(json: JSONObject, ack: IOAcknowledge): Unit = {
    logger.debug("SocketIO message {}", json)
    // response should ack using json
  }

  def emit[T: Encoder, U: Decoder](event: String, json: T): Future[U] = {
    val ret = Promise[U]()
    socket.emit(event, new IOAcknowledge {
      override def ack(objects: AnyRef*): Unit = {
        // TODO : find cleaner and better (maybe it is getting arg as tail ?
        val str = Try {
          objects
            .asInstanceOf[mutable.WrappedArray[AnyRef]]
            .head
            .asInstanceOf[JSONObject]
            .toString
        }
        val json = str.flatMap { str =>
          jawn.decode[U](str) match {
            case Right(obj) => Success(obj)
            case Left(err) => Failure(err)
          }
        }
        ret.complete(json)
      }
    }, new JSONObject(json.asJson.noSpaces))
    ret.future
  }

}

object SpeakUp extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  val socket = new ScalaSocketIO("http://web.speakup.info/")

  sealed trait SpeakUpApi
  class SpeakUpEvent(val event: String)      extends SpeakUpApi
  trait SpeakUpAck                           extends SpeakUpApi
  @JsonCodec case class Peer(dev_id: String) extends SpeakUpEvent("peer")
  @JsonCodec case class PeerAck(user_tags: List[String], min_api_v: String, max_api_v: String, peer_id: String)
      extends SpeakUpAck

  val newPeer = Peer("123")

  // can easily add some more sugar, overload?
  socket.emit[Peer, PeerAck](newPeer.event, newPeer).onComplete {
    case Success(res) =>
      println(res)
    case Failure(err) => err.printStackTrace()
  }

}
