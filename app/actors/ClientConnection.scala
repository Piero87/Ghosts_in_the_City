package actors

import akka.actor._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.util.{Failure, Success}
import play.api.libs.json._
import backend.actors.models._

import common._

/**
 * Factory for [[actors.ClientConnection]] instances.
 * To handle a WebSocket with an actor, we need to give Play a akka.actor.Props object 
 * that describes the actor that Play should create when it receives the WebSocket connection.
 */
object ClientConnection {
  
  def props(name: String, uuid: String, upstream: ActorRef, frontend: ActorRef) = Props(new ClientConnection(name,uuid,upstream,frontend))
  
}

/**
 * Actor ClientConnection implementation class.
 * Any messages received from the client will be sent to the actor, and any messages sent to the actor 
 * supplied by Play will be sent to the client.
 * 
 * @constructor create a new actor with name, uid, websocket, frontendManager actor.
 * @param name 
 * @param uid
 * @param upstream Websocket 
 * @param frontendManager 
 */
class ClientConnection(name: String, uid: String, upstream: ActorRef,frontendManager: ActorRef) extends Actor {
  
  var gameManagerClient: ActorRef = _
  implicit val timeout = Timeout(5 seconds)
  implicit val ec = context.dispatcher
  var game_id = ""
  var team = Team.UNKNOWN
  
  val logger = new CustomLogger("ClientConnection")
  logger.log("Ciao giocatore chiamato " + name + "!")
  
  /**
   * Receive method.
   * It helds all the messages that could be sent to the ClientConnection actor from client or server
   */
  def receive = {
    
    /*
     * Json messages received from the client. 
     * They have to be deserialized and send to the server.
     */
    case msg: JsValue =>
      //logger.log(msg.toString() + " (" + name + ")")
      ((__ \ "event").read[String]).reads(msg) map {
        case "new_game" =>
          val newGameResult: JsResult[NewGameJSON] = msg.validate[NewGameJSON](CommonMessages.newGameReads)
          newGameResult match {
            case s: JsSuccess[NewGameJSON] =>
              var p_pos = s.get.pos
              var game_area_edge = s.get.game_area_edge
              var game_type = s.get.game_type
              logger.log("Player position: " + p_pos)
              var player_info = new PlayerInfo(uid,name,team,p_pos,0,List())
              val future = frontendManager ? NewGame(s.get.name.replaceAll(" ", "_") + "__" + System.currentTimeMillis(),s.get.n_players,player_info,game_area_edge,game_type,self)
              future.onSuccess {
                case GameHandler(game,ref) => 
                  if (ref != null) gameManagerClient = ref
                  game_id = game.id
                  for( player <- game.players) {
                     if (player.uid == uid) {
                       team = player.team
                     } 
                  }
                  var g_json = new GameJSON("game_ready",game)
                  val json = Json.toJson(g_json)(CommonMessages.gameJSONWrites)
                  upstream ! json
              }
            case e: JsError => logger.log("NEW GAME ERROR: " + e.toString() + " FROM " + sender.path)
          }
          
        case "games_list" =>
          val gameListRequest: JsResult[GamesListRequestJSON] = msg.validate[GamesListRequestJSON](CommonMessages.gamesListRequestReads)
          gameListRequest match {
            case s: JsSuccess[GamesListRequestJSON] =>
              var game_type = s.get.g_type
              val future = frontendManager ? GamesListFiltered(game_type)
              future.onSuccess {
                case GamesList(list) =>
                var games_list_json = new GamesListResponseJSON("games_list",list)
                val json = Json.toJson(games_list_json)(CommonMessages.gamesListResponseWrites)
                upstream ! json
              }
            case e: JsError => logger.log("NEW GAME ERROR: " + e.toString() + " FROM " + sender.path)
          }
          
        case "join_game" =>
          val joinGameResult: JsResult[JoinGameJSON] = msg.validate[JoinGameJSON](CommonMessages.joinGameReads)
          joinGameResult match {
            case s: JsSuccess[JoinGameJSON] =>
              var p_pos = s.get.pos
              var player_info = new PlayerInfo(uid,name,team,p_pos,0,List())
              val future = frontendManager ? JoinGame(s.get.game,player_info,self)
              future.onSuccess {
                case GameHandler(game,ref) =>  
                  // logger.log("GameManagerClient path: " + sender.path)
                  if (ref != null) gameManagerClient = ref
                  game_id = game.id
                  for( player <- game.players) {
                     if (player.uid == uid) {
                       team = player.team
                     } 
                  }
                  var g_json = new GameJSON("game_ready",game)
                  val json = Json.toJson(g_json)(CommonMessages.gameJSONWrites)
                  upstream ! json
              }
            case e: JsError => logger.log("JOIN GAME ERROR: " + e.toString() + " FROM " + sender.path)
          }
          
         case "leave_game" =>
           game_id = ""
           gameManagerClient ! LeaveGame(uid)
           
         case "update_player_position" =>
           val updatePositionResult: JsResult[UpdatePositionJSON] = msg.validate[UpdatePositionJSON](CommonMessages.updatePositionReads)
           updatePositionResult match {
            case s: JsSuccess[UpdatePositionJSON] =>
              var PlayerInfo = new PlayerInfo(uid,name, team, s.get.pos,0,List())
              gameManagerClient ! UpdatePosition(PlayerInfo)
            case e: JsError => logger.log("UPDATE PLAYER POSITION ERROR: " + e.toString() + " FROM " + sender.path)
           }
           
         case "resume_game" =>
           val resumeGameResult: JsResult[ResumeGameJSON] = msg.validate[ResumeGameJSON](CommonMessages.resumeGameReads)
           resumeGameResult match {
            case s: JsSuccess[ResumeGameJSON] =>
              var p_pos = s.get.pos
              var player_info = new PlayerInfo(uid,name,team,p_pos,0, List())
              val future = frontendManager ? ResumeGame(s.get.game_id,player_info, self)
              future.onSuccess {
                case GameHandler(game,ref) =>  
                  //logger.log("GameManagerClient path: " + sender.path)
                  if (ref != null) gameManagerClient = ref
                  game_id = game.id
                  for( player <- game.players) {
                     if (player.uid == uid) {
                       team = player.team
                     } 
                  }
                  var g_json = new GameJSON("game_status",game)
                  val json = Json.toJson(g_json)(CommonMessages.gameJSONWrites)
                  upstream ! json
              }
            case e: JsError => logger.log("RESUME GAME ERROR: " + e.toString() + " FROM " + sender.path)
          }
           
         case "set_trap" =>
           gameManagerClient ! SetTrapRequest(uid)
         case "hit_player" =>
           logger.log("Hit Player!")
           gameManagerClient ! HitPlayerRequest(uid)
         case "open_treasure" =>
           gameManagerClient ! OpenTreasureRequest(uid)
           
         // Messages from admin client
         case "admin_login" =>
           val adminLoginResult: JsResult[AdminLoginJSON] = msg.validate[AdminLoginJSON](CommonMessages.adminLoginJSONReads)
           adminLoginResult match {
            case s: JsSuccess[AdminLoginJSON] =>
              var a_name = s.get.name
              var a_pwd = s.get.password
              val future = frontendManager ? AdminLogin(a_name, a_pwd)
              future.onSuccess {
                case LoginResult(result) => 
                  logger.log("Login result received")
                  var l_json = new LoginResultJSON("login_result",result)
                  val json = Json.toJson(l_json)(CommonMessages.loginResultJSONWrites)
                  upstream ! json
              }
            case e: JsError => 
              logger.log("ADMIN LOGIN ERROR: " + e.toString() + " FROM " + sender.path)
           }
         case "started_games_list" =>
           logger.log("Admin game list request received")
           val startedGameListRequest: JsResult[StartedGamesListRequestJSON] = msg.validate[StartedGamesListRequestJSON](CommonMessages.startedGamesListRequestReads)
            startedGameListRequest match {
            case s: JsSuccess[StartedGamesListRequestJSON] =>
              val future = frontendManager ? StartedGamesList
              future.onSuccess {
                case GamesList(list) =>
                var games_list_json = new GamesListResponseJSON("games_list",list)
                val json = Json.toJson(games_list_json)(CommonMessages.gamesListResponseWrites)
                upstream ! json
              }
            case e: JsError => logger.log("STARTED GAMES LIST ERROR: " + e.toString() + " FROM " + sender.path)
          }
      }
      
    /*
     * Messages received from the server. 
     * They have to serialized into json message and send to the client
     * through the upstream actor.
     */
    case GameStatusBroadcast(game: Game) =>
      var g_json = new GameJSON("game_status",game)
      val json = Json.toJson(g_json)(CommonMessages.gameJSONWrites)
      upstream ! json
    case BroadcastUpdatePosition(player: PlayerInfo) =>
      var broadcast_update_pos = new BroadcastUpdatePositionJSON("update_player_position",player)
      val json = Json.toJson(broadcast_update_pos)(CommonMessages.broadcastUpdatePositionWrites)
      upstream ! json
    case BroadcastGhostsPositions(ghosts) =>
      var g_json = new BroadcastGhostsPositionsJSON("update_ghosts_positions",ghosts)
      val json = Json.toJson(g_json)(CommonMessages.broadcastGhostsPositionWrites)
      upstream ! json
    case BroadcastNewTrap(trap) =>
      var g_json = new BroadcastNewTrapJSON("new_trap",trap)
      val json = Json.toJson(g_json)(CommonMessages.broadcastNewTrapJSONWrites)
      upstream ! json
    case BroadcastTrapActivated(trap) =>
      var g_json = new BroadcastTrapActivatedJSON("active_trap",trap)
      val json = Json.toJson(g_json)(CommonMessages.broadcastTrapActivatedJSONWrites)
      upstream ! json
    case BroadcastRemoveTrap(trap) =>
      var g_json = new BroadcastNewTrapJSON("remove_trap",trap)
      val json = Json.toJson(g_json)(CommonMessages.broadcastNewTrapJSONWrites)
      upstream ! json
    case UpdatePlayerInfo(player) =>
      var g_json = new UpdatePlayerInfoJSON("update_player_info",player)
      val json = Json.toJson(g_json)(CommonMessages.updatePlayerInfoJSONWrites)
      upstream ! json
    case MessageCode(uid,code,option) =>
      var g_json = new MessageCodeJSON("message",code,option)
      val json = Json.toJson(g_json)(CommonMessages.messageCodeJSONWrites)
      upstream ! json
    case BroadcastUpdateTreasure(treasures) =>
      var g_json = new UpdateTreasureJSON("update_treasures",treasures)
      val json = Json.toJson(g_json)(CommonMessages.updateTreasureJSONWrites)
      upstream ! json
    case BroadcastVictoryResponse(team,players) =>
      var g_json = new VictoryResponseJSON("game_results",team,players)
      val json = Json.toJson(g_json)(CommonMessages.victoryResponseJSONWrites)
      upstream ! json
  }
  
  /**
   * When the WebSocket has closed, Play will automatically stop the actor. 
   * That method handles this situation like it would be a network disruption and
   * notify that thing to the gameManagerClient.
   */
  override def postStop() = {
    if (game_id != "") {
      gameManagerClient ! PauseGame(uid)
    }
  }
}
