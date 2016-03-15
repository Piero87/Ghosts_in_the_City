package common

import akka.actor._
import scala.collection.mutable.MutableList
import backend.actors.models._

object StatusGame extends Enumeration {
  type StatusGame = Int
   val WAITING = 0
   val STARTED = 1
   val PAUSED = 2
   val FINISHED = 3
   val WITH_ADMIN = 4
} 

object Team extends Enumeration {
  type Team = Int
   val NO_ENOUGH_PLAYER = -2
   val UNKNOWN = -1
   val RED = 0
   val BLUE = 1

}

object GhostMood extends Enumeration {
  type GhostMood = Int
   val CALM = 0
   val ANGRY = 1
   val TRAPPED = 2
}

object Movement extends Enumeration {
  type Movement = Int
   val UP = 0
   val RIGHT = 1
   val DOWN = 2
   val LEFT = 3
   val STILL = -1
}

object TrapStatus extends Enumeration {
  type TrapStatus = Int
   val IDLE = 0
   val ACTIVE = 1
}

object MsgCodes extends Enumeration {
  type MsgCodes = Int
  //Error 
  val NO_TRAP = -1
  val OUT_OF_AREA = -2
  val T_NEEDS_KEY = -3
  val NOT_ENOUGH_PLAYERS = -4
  val T_EMPTY = -5
  val NO_T_NEAR_YOU = -6
  
  // Normal
  val PARANORMAL_ATTACK = 1
  val HUMAN_ATTACK = 2
  val KEY_FOUND = 3
  val GOLD_FOUND = 4
  val K_G_FOUND = 5
  val T_SUCCESS_OPENED = 6
  val BACK_IN_AREA = 7
   
}

object PlayerType extends Enumeration {
  type PlayerType = String
  
  val WEARABLE = "wearable"
  val WEB = "web"
  val UNKNOWN = ""
}

object GameType extends Enumeration {
  type GameType = String
  
  val REALITY = "reality"
  val WEB = "web"
  val UNKNOWN = ""
}

// Game Engine
case class MessageCode(uid: String, code: Int, option: String)
case object Finish

// Ghost messages
case class GhostPositionUpdate(uid: String, pos: Point, mood: Int)
case class GhostInfo(uid: String, level: Int, mood: Int, pos: Point)
case object UpdateGhostsPositions
case object GhostStart
case object GhostPause
case object GhostReleased
case class BroadcastGhostsPositions(ghosts: MutableList[GhostInfo])
case class GhostTrapped(pos: Point)
case class IAttackYou(attacker_uid: String, attack_type: Int, gold_perc_stolen: Double, key_stolen: Int)

// Player 
case object PlayersInfo
case class UpdatePosition(player: PlayerInfo)
case class BroadcastUpdatePosition(player: PlayerInfo)
case class Players(players: MutableList[Tuple2[PlayerInfo, ActorRef]])
case class UpdatePlayerPos(pos: Point)
case class PlayerInfo(uid: String, name: String, team: Int, pos: Point, gold: Int, keys: List[Key])
case class UpdatePlayerInfo(player: PlayerInfo)
case class OpenTreasureRequest(uid: String)
case class OpenTreasure(treasures: List[ActorRef], player: PlayerInfo)
case class GoldStolen(gold: Int)
case class HitPlayerRequest(player_uid: String)
case class AttackHim(player: ActorRef)
case class PlayerAttacked(uid: String, attacker_uid: String, attack_type: Int, gold_perc_stolen: Double, keys_stolen: Int)

// Treasure
case class Open(pos_p: Point, keys: List[Key])
case class IncreaseGold(gold: Int)
case class LootRetrieved(loot: Tuple2[Key,Int])
case class TreasureError(msg : String)
case class TreasureInfo(uid: String, status: Int, pos: Point)
case class TreasureResponse(uid_p: String, results: List[Tuple5[Int,Key,Int,String,String]])
case class BroadcastUpdateTreasure(treasures: MutableList[TreasureInfo])
case class UpdateTreasure(uid: String, status: Int)

// Trap
case class SetTrapRequest(uid: String)
case class SetTrap(gold: Int, pos: Point)
case class TrapInfo(uid: String, pos: Point, status: Int)
case class NewTrap(uid: String, gold: Int, pos: Point)
case class BroadcastNewTrap(trap: TrapInfo)
case class BroadcastTrapActivated(trap: TrapInfo)
case class RemoveTrap(uid: String)
case class BroadcastRemoveTrap(trap: TrapInfo)

// Game
case class GameStatusBroadcast(game: Game)
case class JoinGame(game: Game, player: PlayerInfo, ref: ActorRef = null)
case class ResumeGame(game_id: String, player: PlayerInfo, ref: ActorRef)
case class GameHandler(game: Game, ref: ActorRef = null)
case class LeaveGame(uid: String)
case class PauseGame(uid: String)
case class NewGame(name: String, n_players: Int, player: PlayerInfo, game_area_edge: Double, game_type: String, ref: ActorRef = null)
case class Game(id: String, name: String, n_players: Int, status: Int, g_type: String, players: MutableList[PlayerInfo], ghosts: MutableList[GhostInfo], treasures: MutableList[TreasureInfo], traps: MutableList[TrapInfo])
case class GamesList(list: List[Game])
case class GamesListFiltered(game_type: String)
case object GameStatus
case class BroadcastVictoryResponse(team: Int,players: List[PlayerInfo])

// Actor
case object KillYourself
case object KillMyself
case object CheckPaused

// All admin stuff messages
case class AdminLogin(name: String, password: String)
case class LoginResult(result: Boolean)
case object StartedGamesList
case class UpdateInfo(game: Game, adminuid: String)

// Json
case class NewGameJSON(event: String, name: String, pos: Point, game_area_edge: Double, n_players: Int, game_type: String)
case class GameJSON(event: String, game: Game)
case class GamesListResponseJSON(event: String, list: List[Game])
case class GamesListRequestJSON(event: String, g_type: String)
case class JoinGameJSON(event: String, game: Game, pos: Point)
case class LeaveGameJSON(event: String)
case class ResumeGameJSON(event:String, game_id: String, pos: Point)
case class UpdatePositionJSON(event: String, pos: Point)
case class BroadcastUpdatePositionJSON(event: String, player: PlayerInfo)
case class BroadcastGhostsPositionsJSON(event: String, ghosts: MutableList[GhostInfo])
case class SetTrapJSON(event: String)
case class BroadcastNewTrapJSON(event: String, trap: TrapInfo)
case class BroadcastTrapActivatedJSON(event:String, trap: TrapInfo)
case class BroadcastRemoveTrapJSON(event:String, trap: TrapInfo)
case class UpdatePlayerInfoJSON(event: String, player: PlayerInfo)
case class MessageCodeJSON(event: String, code: Int, option: String)
case class UpdateTreasureJSON(event: String, treasures: MutableList[TreasureInfo])
case class VictoryResponseJSON(event: String, team: Int, players: List[PlayerInfo])

// Admin Json
case class AdminLoginJSON(event: String, name: String, password: String)
case class LoginResultJSON(event: String, result: Boolean)
case class StartedGamesListRequestJSON(event: String)
    
import play.api.libs.json._

/**
 * All message serializer
 */
object CommonMessages {

  implicit val pointReads = Json.reads[Point]
  implicit val pointWrites = Json.writes[Point]
  
  implicit val keyReads = Json.reads[Key]
  implicit val keyWrites = Json.writes[Key]
  
  implicit val playerInfoReads = Json.reads[PlayerInfo]
  implicit val playerInfoWrites = Json.writes[PlayerInfo]
  
  implicit val leaveGameReads = Json.reads[LeaveGameJSON]
  implicit val leaveGameWrites = Json.writes[LeaveGameJSON]
  
  implicit val ghostInfoReads = Json.reads[GhostInfo]
  implicit val ghostInfoWrites = Json.writes[GhostInfo]
  
  implicit val treasureInfoReads = Json.reads[TreasureInfo]
  implicit val treasureInfoWrites = Json.writes[TreasureInfo]
  
  implicit val trapReads = Json.reads[TrapInfo]
  implicit val trapWrites = Json.writes[TrapInfo]
  
  implicit val gameReads = Json.reads[Game]
  implicit val gameWrites = Json.writes[Game]
  
  implicit val gameJSONReads = Json.reads[GameJSON]
  implicit val gameJSONWrites = Json.writes[GameJSON]
  
  implicit val newGameReads = Json.reads[NewGameJSON]
  implicit val newGameWrites = Json.writes[NewGameJSON]
  
  implicit val gamesListResponseReads = Json.reads[GamesListResponseJSON]
  implicit val gamesListResponseWrites = Json.writes[GamesListResponseJSON]
  
  implicit val gamesListRequestReads = Json.reads[GamesListRequestJSON]
  implicit val gamesListRequestWrites = Json.writes[GamesListRequestJSON]
  
  implicit val joinGameReads = Json.reads[JoinGameJSON]
  implicit val joinGameWrites = Json.writes[JoinGameJSON]
  
  implicit val resumeGameReads = Json.reads[ResumeGameJSON]
  implicit val resumeGameWrites = Json.writes[ResumeGameJSON]
  
  implicit val updatePositionReads = Json.reads[UpdatePositionJSON]
  implicit val updatePositionWrites = Json.writes[UpdatePositionJSON]
  
  implicit val broadcastUpdatePositionReads = Json.reads[BroadcastUpdatePositionJSON]
  implicit val broadcastUpdatePositionWrites = Json.writes[BroadcastUpdatePositionJSON]
  
  implicit val broadcastGhostsPositionReads = Json.reads[BroadcastGhostsPositionsJSON]
  implicit val broadcastGhostsPositionWrites = Json.writes[BroadcastGhostsPositionsJSON]
  
  implicit val setTrapReads = Json.reads[SetTrapJSON]
  implicit val setTrapWrites = Json.writes[SetTrapJSON]
  
  implicit val broadcastNewTrapJSONReads = Json.reads[BroadcastNewTrapJSON]
  implicit val broadcastNewTrapJSONWrites = Json.writes[BroadcastNewTrapJSON]
  
  implicit val broadcastTrapActivatedJSONReads = Json.reads[BroadcastTrapActivatedJSON]
  implicit val broadcastTrapActivatedJSONWrites = Json.writes[BroadcastTrapActivatedJSON]
  
  implicit val broadcastRemoveTrapJSONReads = Json.reads[BroadcastRemoveTrapJSON]
  implicit val broadcastRemoveTrapJSONWrites = Json.writes[BroadcastRemoveTrapJSON]
  
  implicit val updatePlayerInfoJSONReads = Json.reads[UpdatePlayerInfoJSON]
  implicit val updatePlayerInfoJSONWrites = Json.writes[UpdatePlayerInfoJSON]
  
  implicit val messageCodeJSONReads = Json.reads[MessageCodeJSON]
  implicit val messageCodeJSONWrites = Json.writes[MessageCodeJSON]
  
  implicit val updateTreasureJSONReads = Json.reads[UpdateTreasureJSON]
  implicit val updateTreasureJSONWrites = Json.writes[UpdateTreasureJSON]
  
  implicit val victoryResponseJSONReads = Json.reads[VictoryResponseJSON]
  implicit val victoryResponseJSONWrites = Json.writes[VictoryResponseJSON]
  
  implicit val adminLoginJSONReads = Json.reads[AdminLoginJSON]
  implicit val adminLoginJSONWrites = Json.writes[AdminLoginJSON]
  
  implicit val loginResultJSONReads = Json.reads[LoginResultJSON]
  implicit val loginResultJSONWrites = Json.writes[LoginResultJSON]
  
  implicit val startedGamesListRequestReads = Json.reads[StartedGamesListRequestJSON]
  implicit val startedGamesListRequestWrites = Json.writes[StartedGamesListRequestJSON]
  
}
