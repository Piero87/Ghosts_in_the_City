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

class ClientConnection(username: String, uid: String, upstream: ActorRef,frontendManager: ActorRef) extends Actor {
  
  var gameManagerClient: ActorRef = _
  implicit val timeout = Timeout(5 seconds)
  implicit val ec = context.dispatcher
  var game_id = ""
  var team = Team.UNKNOWN
  
  def receive = {
    case msg: JsValue =>
      Logger.info(msg.toString())
      ((__ \ "event").read[String]).reads(msg) map {
        case "new_game" =>
          val newGameResult: JsResult[NewGameJSON] = msg.validate[NewGameJSON](CommonMessages.newGameReads)
          newGameResult match {
            case s: JsSuccess[NewGameJSON] => 
              var user_info = new UserInfo(uid,username,team,0,0)
              val future = frontendManager ? NewGame(s.get.name.replaceAll(" ", "_")+"_"+System.currentTimeMillis(),s.get.n_players,user_info,self)
              future.onSuccess {
                case GameHandler(game,ref) => 
                  Logger.info ("ClientConnection: Frontend Game Manager path: "+sender.path)
                  if (ref != null) gameManagerClient = ref
                  game_id = game.id
                  for( a <- 0 to game.players.size) {
                     if (game.players(a).uid == uid) {
                       team = game.players(a).team
                     } 
                  }
                  var g_json = new GameJSON("game_ready",game)
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
              var games_list_json = new GamesListResponseJSON("games_list",list)
              val json = Json.toJson(games_list_json)(CommonMessages.gamesListResponseWrites)
              upstream ! json
          }
        case "join_game" =>
          val joinGameResult: JsResult[JoinGameJSON] = msg.validate[JoinGameJSON](CommonMessages.joinGameReads)
          joinGameResult match {
            case s: JsSuccess[JoinGameJSON] =>
              var user_info = new UserInfo(uid,username,team,0,0)
              val future = frontendManager ? JoinGame(s.get.game,user_info,self)
              future.onSuccess {
                case GameHandler(game,ref) =>  
                  Logger.info ("ClientConnection: Frontend Game Manager path: "+sender.path)
                  if (ref != null) gameManagerClient = ref
                  game_id = game.id
                  for( a <- 1 to game.players.size) {
                     if (game.players(a).uid == uid) {
                       team = game.players(a).team
                     } 
                  }
                  var g_json = new GameJSON("game_ready",game)
                  val json = Json.toJson(g_json)(CommonMessages.gameJSONWrites)
                  upstream ! json
              }
            case e: JsError => 
              Logger.info("Ops JoinGame: "+e.toString())
          }
         case "leave_game" =>
           Logger.info("CC: LeaveGame request")
           game_id = ""
           var userInfo = new UserInfo(uid,username,team, 0,0)
           gameManagerClient ! LeaveGame(userInfo)
         case "update_position" =>
           val updatePositionResult: JsResult[UpdatePositionJSON] = msg.validate[UpdatePositionJSON](CommonMessages.updatePositionReads)
           updatePositionResult match {
            case s: JsSuccess[UpdatePositionJSON] =>
              var userInfo = new UserInfo(uid,username,team, s.get.x,s.get.y)
              gameManagerClient ! UpdatePosition(userInfo)
            case e: JsError => 
              Logger.info("Ops JoinGame: "+e.toString())
           }
           
      }
    case GameStatusBroadcast(game: Game) =>
      var g_json = new GameJSON("game_status",game)
      val json = Json.toJson(g_json)(CommonMessages.gameJSONWrites)
      upstream ! json
    case BroadcastUpdatePosition(user: UserInfo) =>
      var broadcast_update_pos = new BroadcastUpdatePositionJSON("update_position",user)
      val json = Json.toJson(broadcast_update_pos)(CommonMessages.broadcastUpdatePositionWrites)
      upstream ! json
  }
  
  override def postStop() = {
    if (game_id != "") {
      var userInfo = new UserInfo(uid,username,team, 0,0)
      gameManagerClient ! LeaveGame(userInfo)
    }
  }
}
