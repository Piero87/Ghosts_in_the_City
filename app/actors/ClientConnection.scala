package actors

import akka.actor._
import play.api.Logger
import play.extras.geojson.LatLng
import play.extras.geojson.Point
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.util.{Failure, Success}
import play.api.libs.json._

import common._

object ClientConnection {
  
  def props(username: String, upstream: ActorRef, frontend: ActorRef) = Props(new ClientConnection(username,upstream,frontend))
  
}

class ClientConnection(username: String, upstream: ActorRef,frontendManager: ActorRef) extends Actor {
  
  var gameManagerClient: ActorRef = _
  
  def receive = {
    case msg: JsValue =>
      ((__ \ "event").read[String]).reads(msg) map {
        case "new_game" =>
          val newGameResult: JsResult[NewGameJSON] = msg.validate[NewGameJSON](CommonMessages.newGameReads)
          newGameResult match {
            case s: JsSuccess[NewGameJSON] => Logger.info("Name: " + s.get.name)
            case e: JsError => Logger.info("Ops") 
          }
        case _ =>
          Logger.info("quello che va per la maggiore")
      }
//    case NewGame(id,name) => 
//      Logger.info("ClientConnection: NewGame received")
//      implicit val timeout = Timeout(5 seconds)
//      implicit val ec = context.dispatcher
//      val future = frontendManager ? NewGame(id,name.replaceAll(" ", "_")+"_"+System.currentTimeMillis())
//      future.onSuccess { 
//        case result: ActorRef => 
//          Logger.info ("ClientConnection NewGame result: "+result.path)
//          gameManagerClient = result
//          upstream ! NewGame(id,name)
//      }
//      future onFailure {
//        case e: Exception => Logger.info("****** ERRORE ******")
//      }
  }
}
