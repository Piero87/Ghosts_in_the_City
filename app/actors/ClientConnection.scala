package actors

import javax.inject.{Named, Inject}

import akka.actor.{ActorRef, Actor}
import com.google.inject.assistedinject.Assisted
import play.extras.geojson._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.WebSocket.FrameFormatter
import play.api.Logger

object ClientConnection {

  /**
   * The factory interface for creating client connections
   */
  trait Factory {
    def apply(email: String, upstream: ActorRef): Actor
  }

  /**
   * Events to/from the client side
   */
  sealed trait ClientEvent

  /**
   * Event sent from the client when they have moved
   */
  case class UserMoved(position: Point[LatLng]) extends ClientEvent


  /*
   * JSON serialisers/deserialisers for the above messages
   */

  object ClientEvent {
    implicit def clientEventFormat: Format[ClientEvent] = Format(
      (__ \ "event").read[String].flatMap {
        case "user-moved" => UserMoved.userMovedFormat.map(identity)
        case other => Reads(_ => JsError("Unknown client event: " + other))
      },
      Writes {
        case um: UserMoved => UserMoved.userMovedFormat.writes(um)
      }
    )

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
  }

  object UserMoved {
    implicit def userMovedFormat: Format[UserMoved] = (
      (__ \ "event").format[String] ~
        (__ \ "position").format[Point[LatLng]]
      ).apply({
      case ("user-moved", position) => UserMoved(position)
    }, userMoved => ("user-moved", userMoved.position))
  }

}

/**
 * Represents a client connection
 *
 * @param email The email address of the client
 * @param regionManagerClient The region manager client to send updates to
 */
class ClientConnection @Inject() (@Assisted email: String, @Assisted upstream: ActorRef) extends Actor {

  // Create the subscriber actor to subscribe to position updates
  //val subscriber = context.actorOf(PositionSubscriber.props(self), "positionSubscriber")

  import ClientConnection._

  def receive = {
    // The users has moved their position, publish to the region
    case UserMoved(point) =>
      Logger.debug("email: "+email+" now: "+ System.currentTimeMillis()+" GPS: "+point.coordinates) 
  }
}
