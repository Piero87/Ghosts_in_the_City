package actors

import akka.actor._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.util.{Failure, Success}
import play.api.libs.json._
import backend.actors.models._

import common._

object ClientConnection {
  
  def props(name: String, uuid: String, upstream: ActorRef, frontend: ActorRef) = Props(new ClientConnection(name,uuid,upstream,frontend))
  
}

class ClientConnection(name: String, uid: String, upstream: ActorRef,frontendManager: ActorRef) extends Actor {
  
  var gameManagerClient: ActorRef = _
  implicit val timeout = Timeout(5 seconds)
  implicit val ec = context.dispatcher
  var game_id = ""
  var team = Team.UNKNOWN
  var player_type = PlayerType.UNKNOWN
  
  val logger = new CustomLogger("ClientConnection")
  logger.log("Ciao mondo! L'utente " + name + " si è appena collegato!")
  
  def receive = {
    case msg: JsValue =>
      //logger.log(msg.toString() + " (" + name + ")")
      ((__ \ "event").read[String]).reads(msg) map {
        case "new_game" =>
          val newGameResult: JsResult[NewGameJSON] = msg.validate[NewGameJSON](CommonMessages.newGameReads)
          newGameResult match {
            case s: JsSuccess[NewGameJSON] =>
              var p_pos = s.get.pos
              if(p_pos.latitude != 0.0 && p_pos.longitude != 0.0){
                // Verifico che la posizione che mi è stata inviata sia diversa da 0.0,0.0(quella difeaul del web) per determinare se il client
                // è un wearable o no. Se è wearable lo scrivo in player info e gli inserisco la posizione che mi è stata inviata dal client
                player_type = PlayerType.WEARABLE
              }else{
                player_type = PlayerType.WEB
              }
              var player_info = new PlayerInfo(uid,name,player_type,team,p_pos,0,List())
              val future = frontendManager ? NewGame(s.get.name.replaceAll(" ", "_") + "__" + System.currentTimeMillis(),s.get.n_players,player_info,self)
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
              var p_pos = s.get.pos
              if(p_pos.latitude != 0.0 && p_pos.longitude != 0.0){
                // Verifico che la posizione che mi è stata inviata sia diversa da 0.0,0.0(quella difeaul del web) per determinare se il client
                // è un wearable o no. Se è wearable lo scrivo in player info e gli inserisco la posizione che mi è stata inviata dal client
                player_type = PlayerType.WEARABLE
              }else{
                player_type = PlayerType.WEB
              }
              var player_info = new PlayerInfo(uid,name,player_type,team,p_pos,0,List())
              if(p_pos.latitude != 0.0 && p_pos.longitude != 0.0){
                // Verifico che la posizione che mi è stata inviata sia diversa da 0.0,0.0(quella difeaul del web) per determinare se il client
                // è un wearable o no. Se è wearable lo scrivo in player info e gli inserisco la posizione che mi è stata inviata dal client
                player_info = new PlayerInfo(uid,name,PlayerType.WEARABLE,team,p_pos,0,List())
              }
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
              var PlayerInfo = new PlayerInfo(uid,name,player_type, team, s.get.pos,0,List())
              gameManagerClient ! UpdatePosition(PlayerInfo)
            case e: JsError => logger.log("UPDATE PLAYER POSITION ERROR: " + e.toString() + " FROM " + sender.path)
           }
           
         case "resume_game" =>
           val resumeGameResult: JsResult[ResumeGameJSON] = msg.validate[ResumeGameJSON](CommonMessages.resumeGameReads)
           resumeGameResult match {
            case s: JsSuccess[ResumeGameJSON] =>
              var p_pos = s.get.pos
              var player_info = new PlayerInfo(uid,name,player_type,team,p_pos,0, List())
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
           
      }
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
  
  override def postStop() = {
    if (game_id != "") {
      gameManagerClient ! PauseGame(uid)
    }
  }
}
