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
  
  def props(username: String, uuid: String, upstream: ActorRef, frontend: ActorRef) = Props(new ClientConnection(username,uuid,upstream,frontend))
  
}

class ClientConnection(username: String, uuid: String, upstream: ActorRef,frontendManager: ActorRef) extends Actor {
  
  var gameManagerClient: ActorRef = _
  implicit val timeout = Timeout(5 seconds)
  implicit val ec = context.dispatcher
  
  def receive = {
    case msg: JsValue =>
      Logger.info(msg.toString())
      ((__ \ "event").read[String]).reads(msg) map {
        case "new_game" =>
          val newGameResult: JsResult[NewGameJSON] = msg.validate[NewGameJSON](CommonMessages.newGameReads)
          newGameResult match {
            case s: JsSuccess[NewGameJSON] => 
              val future = frontendManager ? NewGame(s.get.name.replaceAll(" ", "_")+"_"+System.currentTimeMillis(),s.get.n_players,s.get.user,self)
              future.onSuccess {
                case GameHandler(game,ref) => 
                  Logger.info ("ClientConnection: Frontend Game Manager path: "+sender.path)
                  if (ref != null) gameManagerClient = ref
                  var player_info_fake = new UserInfo("0","server","")
                  var g_json = new GameJSON("game_ready",game,player_info_fake)
                  val json = Json.toJson(g_json)(CommonMessages.gameJSONWrites)
                  upstream ! json
              }
            case e: JsError => 
              Logger.info("Ops NewGame: "+e.toString())
          }
          
        case "games_list" =>
          val future = frontendManager ? GamesList
          future.onSuccess {
            case GamesList(list) => 
              var player_info_fake = new UserInfo("0","server","")
              var games_list_json = new GamesListJSON("games_list",list,player_info_fake)
              val json = Json.toJson(games_list_json)(CommonMessages.gamesListWrites)
              upstream ! json
          }
        case "join_game" =>
          val joinGameResult: JsResult[JoinGameJSON] = msg.validate[JoinGameJSON](CommonMessages.joinGameReads)
          joinGameResult match {
            case s: JsSuccess[JoinGameJSON] =>
              val future = frontendManager ? JoinGame(s.get.game,s.get.user,self)
              future.onSuccess {
                case Game(id,name,n_players,status,players) => 
                  Logger.info ("ClientConnection: Frontend Game Manager path: "+sender.path)
                  var g = new Game(id,name,n_players,status,players)
                  var player_info_fake = new UserInfo("0","server","")
                  var g_json = new GameJSON("game_ready",g,player_info_fake)
                  val json = Json.toJson(g_json)(CommonMessages.gameJSONWrites)
                  upstream ! json
              }
            case e: JsError => 
              Logger.info("Ops JoinGame: "+e.toString())
          }
      }
    case GameStatusBroadcast(game: Game) =>
      var g = new Game(game.id,game.name,game.n_players,game.status,game.players)
      var player_info_fake = new UserInfo("0","server","")
      var g_json = new GameJSON("game_status",g,player_info_fake)
      val json = Json.toJson(g_json)(CommonMessages.gameJSONWrites)
      upstream ! json
  }
}
