package actors

import akka.actor._
import play.api.Logger
import akka.util.Timeout

case object Ping
case object Pong
case object BackendRegistration

class Frontend extends Actor {
  
  var backends = IndexedSeq.empty[ActorRef]
  
  def receive = {
    case Ping =>
      Logger.info("Frontend - Ping request received")
    case BackendRegistration if !backends.contains(sender()) =>
      context watch sender()
      backends = backends :+ sender()

    case Terminated(a) =>
      backends = backends.filterNot(_ == a)
  }
}