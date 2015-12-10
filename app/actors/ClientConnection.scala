package actors

import akka.actor._
import play.api.Logger

object ClientConnection {
  def props(email: String, out: ActorRef) = Props(new ClientConnection(email,out))
}

class ClientConnection(email: String, out: ActorRef) extends Actor {
  def receive = {
    case gps_position : String =>
      Logger.info(gps_position)
      out ! ("I received this position: " + gps_position)
  }
}
