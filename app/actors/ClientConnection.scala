package actors

import akka.actor._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter

import play.extras.geojson.LatLng
import play.extras.geojson.Point
import play.api.libs.functional.syntax._

object ClientConnection {
  
  /**
   * Events to/from the client side
   */
  sealed trait ClientEvent
  
  /**
   * Event sent from the client when they have moved
   */
  case class UserMoved(position: Point[LatLng]) extends ClientEvent
  
  /**
	 * A user position
	 */
  case class UserPosition(id: String, timestamp: Long, position: LatLng)

  def props(email: String, upstream: ActorRef) = Props(new ClientConnection(email,upstream))
  
  /**
   * Formats WebSocket frames to be ClientEvents.
   */
  implicit def clientEventFrameFormatter: FrameFormatter[ClientEvent] = FrameFormatter.jsonFrame.transform(
    clientEvent => Json.toJson(clientEvent),
    json => Json.fromJson[ClientEvent](json).fold(
      invalid => throw new RuntimeException("Bad client event on WebSocket: " + invalid),
      valid => valid
    )
  )
  
  /**
   * JSON serialisers/deserialisers for the above messages
   */
  implicit def clientEventFormat: Format[ClientEvent] = Format(
    (__ \ "event").read[String].flatMap {
      case "user-moved" => UserMoved.userMovedFormat.map(identity)
      case other => Reads(_ => JsError("Unknown client event: " + other))
    },
    Writes {
      case um: UserMoved => UserMoved.userMovedFormat.writes(um)
    }
  )
  
  object UserMoved {
    implicit def userMovedFormat: Format[UserMoved] = (
      (__ \ "event").format[String] and
          (__ \ "position").format[Point[LatLng]]
      ).apply({
      case ("user-moved", position) => UserMoved(position)
    }, (userMoved: UserMoved) => ("user-moved", userMoved.position))
  }
  
}

class ClientConnection(email: String, upstream: ActorRef) extends Actor {
  
  import ClientConnection._
  
  def receive = {
    case UserMoved(point) =>
      Logger.info("Received event UserMoved from: "+email+" with position: "+point.coordinates.lat+" - "+point.coordinates.lng+" at "+System.currentTimeMillis())
      upstream ! UserPosition(email, System.currentTimeMillis(), point.coordinates)
  }
}
