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
  
  case class UserPing(unused: String) extends ClientEvent
  
  /**
   * Event sent from the client when they have moved
   */
  case class UserMoved(position: Point[LatLng]) extends ClientEvent
  
  /**
	 * Event sent from the server when position is received
	 */
  case class UserPosition(id: String, timestamp: Long, position: Point[LatLng]) extends ClientEvent

  def props(email: String, upstream: ActorRef, frontend: ActorRef) = Props(new ClientConnection(email,upstream,frontend))
  
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
      case "user-position" => UserPosition.userPositionFormat.map(identity)
      case "user-ping" => UserPing.userPingFormat.map(identity)
      case other => Reads(_ => JsError("Unknown client event: " + other))
    },
    Writes {
      case um: UserMoved => UserMoved.userMovedFormat.writes(um)
      case up: UserPosition => UserPosition.userPositionFormat.writes(up)
      case pi: UserPing => UserPing.userPingFormat.writes(pi)
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
  
  object UserPosition {
    implicit def userPositionFormat: Format[UserPosition] = (
      (__ \ "event").format[String] and 
        (__ \ "id").format[String] and
          (__ \ "timestamp").format[Long] and
            (__ \ "position").format[Point[LatLng]]
      ).apply({
      case ("user-position",id,timestamp,position) => UserPosition(id, timestamp, position)
    }, userPosition => ("user-position",userPosition.id, userPosition.timestamp, userPosition.position))
  }
  
  object UserPing {
    implicit def userPingFormat: Format[UserPing] = (
      (__ \ "event").format[String] and
          (__ \ "unused").format[String]
      ).apply({
      case ("user-ping", unused) => UserPing(unused)
    }, (userPing: UserPing) => ("user-ping", userPing.unused))
  }
  
}

class ClientConnection(email: String, upstream: ActorRef,frontend: ActorRef) extends Actor {
  
  import ClientConnection._
  
  def receive = {
    case UserMoved(point) =>
      Logger.info("Received event UserMoved from: "+email+" with position: "+point.coordinates.lat+" - "+point.coordinates.lng+" at "+System.currentTimeMillis())
      upstream ! UserPosition(email, System.currentTimeMillis(), point)
    case UserPing(unused) =>
      frontend ! Ping
  }
}
