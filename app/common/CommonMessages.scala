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
} 

object Team extends Enumeration {
  type Team = Int
   val UNKNOWN = -1
   val BLUE = 0
   val RED = 1
}

object GhostMood extends Enumeration {
  type GhostMood = Int
   val CALM = 0
   val ANGRY = 1
   val TRAPPED = 2
}

object TrapStatus extends Enumeration {
  type TrapStatus = Int
   val IDLE = 0
   val ACTIVE = 1
}

object MsgCodes extends Enumeration {
  type MsgCodes = Int
   val NO_TRAP = -1
}

// Code messages
case class MessageCode(uid: String, code: Int)

// Ghost messages
case class GhostPositionUpdate(uid: String, pos: Point, mood: Int)
case class GhostInfo(uid: String, level: Int, mood: Int, pos: Point)
case object UpdateGhostsPositions
case object GhostStart
case object GhostPause
case object GhostReleased
case class BroadcastGhostsPositions(ghosts: MutableList[GhostInfo])
case class GhostTrapped(pos: Point)

// Player 
case object PlayersPositions
case class UpdatePosition(user: UserInfo)
case class BroadcastUpdatePosition(user: UserInfo)
case class Players(players: MutableList[UserInfo])
case class UpdatePlayerPos(pos: Point)
case class UserInfo(uid: String, name: String, team: Int, pos: Point, gold: Int, keys: List[Key])
case class UpdateUserInfo(user: UserInfo)

// Treasure
case class Open(keys: List[Key])
case class IncreaseGold(gold: Int)
case class LootRetrieved(loot: Tuple2[Key,Int])
case class TreasureError(msg : String)
case class TreasureInfo(uid: String, status: Int, pos: Point)

// Trap
case class SetTrapRequest(user: UserInfo)
case class SetTrap(gold: Int, pos: Point)
case class TrapInfo(uid: String, pos: Point, status: Int)
case class NewTrap(uid: String, gold: Int, pos: Point)
case class BroadcastNewTrap(trap: TrapInfo)
case class BroadcastTrapActivated(trap: TrapInfo)
case class RemoveTrap(uid: String)
case class BroadcastRemoveTrap(trap: TrapInfo)

// Game
case class GameStatusBroadcast(game: Game)
case class JoinGame(game: Game, user: UserInfo, ref: ActorRef = null)
case class ResumeGame(game_id: String, user: UserInfo, ref: ActorRef)
case class GameHandler(game: Game, ref: ActorRef = null)
case class LeaveGame(user: UserInfo)
case class PauseGame(user: UserInfo)
case class NewGame(name: String, n_players: Int, user: UserInfo, ref: ActorRef = null)
case class Game(id: String, name: String, n_players: Int, status: Int, players: MutableList[UserInfo], ghosts: MutableList[GhostInfo], treasures: MutableList[TreasureInfo])
case class GamesList(list: List[Game])
case object GamesList
case object GameStatus

// Actor
case object KillYourself
case object KillMyself
case object CheckPaused


// Json
case class NewGameJSON(event: String, name: String, n_players: Int)
case class GameJSON(event: String, game: Game)
case class GamesListResponseJSON(event: String, list: List[Game])
case class GamesListRequestJSON(event: String)
case class JoinGameJSON(event: String, game: Game)
case class LeaveGameJSON(event: String)
case class ResumeGameJSON(event:String, game_id: String)
case class UpdatePositionJSON(event: String, pos: Point)
case class BroadcastUpdatePositionJSON(event: String, user: UserInfo)
case class BroadcastGhostsPositionsJSON(event: String, ghosts: MutableList[GhostInfo])
case class SetTrapJSON(event: String)
case class BroadcastNewTrapJSON(event: String, trap: TrapInfo)
case class BroadcastTrapActivatedJSON(event:String, trap: TrapInfo)
case class BroadcastRemoveTrapJSON(event:String, trap: TrapInfo)
case class UpdateUserInfoJSON(event: String, user: UserInfo)
case class MessageCodeJSON(event: String, code: Int)

import play.api.libs.json._

object CommonMessages {

  implicit val pointReads = Json.reads[Point]
  implicit val pointWrites = Json.writes[Point]
  
  implicit val keyReads = Json.reads[Key]
  implicit val keyWrites = Json.writes[Key]
  
  implicit val playerInfoReads = Json.reads[UserInfo]
  implicit val playerInfoWrites = Json.writes[UserInfo]
  
  implicit val leaveGameReads = Json.reads[LeaveGameJSON]
  implicit val leaveGameWrites = Json.writes[LeaveGameJSON]
  
  implicit val ghostInfoReads = Json.reads[GhostInfo]
  implicit val ghostInfoWrites = Json.writes[GhostInfo]
  
  implicit val treasureInfoReads = Json.reads[TreasureInfo]
  implicit val treasureInfoWrites = Json.writes[TreasureInfo]
  
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
  
  implicit val trapReads = Json.reads[TrapInfo]
  implicit val trapWrites = Json.writes[TrapInfo]
  
  implicit val setTrapReads = Json.reads[SetTrapJSON]
  implicit val setTrapWrites = Json.writes[SetTrapJSON]
  
  implicit val broadcastNewTrapJSONReads = Json.reads[BroadcastNewTrapJSON]
  implicit val broadcastNewTrapJSONWrites = Json.writes[BroadcastNewTrapJSON]
  
  implicit val broadcastTrapActivatedJSONReads = Json.reads[BroadcastTrapActivatedJSON]
  implicit val broadcastTrapActivatedJSONWrites = Json.writes[BroadcastTrapActivatedJSON]
  
  implicit val broadcastRemoveTrapJSONReads = Json.reads[BroadcastRemoveTrapJSON]
  implicit val broadcastRemoveTrapJSONWrites = Json.writes[BroadcastRemoveTrapJSON]
  
  implicit val updateUserInfoJSONReads = Json.reads[UpdateUserInfoJSON]
  implicit val updateUserInfoJSONWrites = Json.writes[UpdateUserInfoJSON]
  
  implicit val messageCodeJSONReads = Json.reads[MessageCodeJSON]
  implicit val messageCodeJSONWrites = Json.writes[MessageCodeJSON]
  
}
