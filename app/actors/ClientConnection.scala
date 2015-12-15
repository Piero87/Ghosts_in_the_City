package actors

import akka.actor._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter

import play.extras.geojson.LatLng
import play.extras.geojson.Point
import play.api.libs.functional.syntax._

import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.util.{Failure, Success}

object ClientConnection {
  
  def props(username: String, upstream: ActorRef, frontend: ActorRef) = Props(new ClientConnection(username,upstream,frontend))
  
  /**
   * Events to/from the client side
   */
  sealed trait ClientEvent
  
  /**
   * Event sent from the client when they have moved
   */
  case class NewGame(name: String) extends ClientEvent
  
  /**
   * Event sent from the client when they have moved
   */
  case class UserMoved(position: Point[LatLng]) extends ClientEvent
  
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
      case "new-game" => NewGame.newGameFormat.map(identity)
      case other => Reads(_ => JsError("Unknown client event: " + other))
    },
    Writes {
      case um: UserMoved => UserMoved.userMovedFormat.writes(um)
      case ng: NewGame => NewGame.newGameFormat.writes(ng)
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
  
  object NewGame {
    implicit def newGameFormat: Format[NewGame] = (
      (__ \ "event").format[String] and
          (__ \ "name").format[String]
      ).apply({
      case ("new-game", name) => NewGame(name)
    }, (newGame: NewGame) => ("new-game", newGame.name))
  }
}

class ClientConnection(username: String, upstream: ActorRef,frontendManager: ActorRef) extends Actor {
  
  import ClientConnection._
  
  var managerClient: ActorRef = _
  
  def receive = {
//    case UserMoved(point) =>
//      Logger.info("Received event UserMoved from: "+username+" with position: "+point.coordinates.lat+" - "+point.coordinates.lng+" at "+System.currentTimeMillis())
//      upstream ! UserPosition(username, System.currentTimeMillis(), point)
    case NewGame(name) =>
      Logger.info("ClientConnection: NewGame request")
      implicit val timeout = Timeout(5 seconds)
      implicit val ec = context.dispatcher
      frontendManager ? NewGame(name) andThen {
        case Success(_) => 
          managerClient = sender()
          upstream ! NewGame(name)
        case Failure(_) => Logger.info("Errore ClientConnection Creazione Partita")
      }
      
      //managerClient = frontendManager ? NewGame(name) //Qui mi aspetterò l'actorRef di ManagerClient con cui parlerò 
  }
}
