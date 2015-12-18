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
          Logger.info("ClientConnection: NewGame received")
          implicit val timeout = Timeout(5 seconds)
          implicit val ec = context.dispatcher
          val newGameResult: JsResult[NewGameJSON] = msg.validate[NewGameJSON](CommonMessages.newGameReads)
          newGameResult match {
            case s: JsSuccess[NewGameJSON] => 
              val future = frontendManager ? NewGame(s.get.name.replaceAll(" ", "_")+"_"+System.currentTimeMillis(),s.get.n_players)
              future.onSuccess {
                case Game(id,name,n_players) => 
                  Logger.info ("ClientConnection: Frontend Game Manager path: "+sender.path)
                  gameManagerClient = sender
                  var g = new Game(id,name,n_players)
                  var g_json = new GameJSON("game_ready",g)
                  val json = Json.toJson(g_json)(CommonMessages.gameJSONWrites)
                  upstream ! json
              }
            case e: JsError => Logger.info("Ops")
          }
          
          
        case _ =>
          Logger.info(msg.toString())
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
