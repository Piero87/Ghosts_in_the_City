package common

import akka.actor._

object StatusGame extends Enumeration {
  type StatusGame = Int
   val WAITING = 0
   val STARTED = 1
   val PAUSED = 2
   val FINISHED = 3
} 

object Team extends Enumeration {
  type Team = Int
   val BLUE = 0
   val RED = 1
   val UNKNOWN = -1
} 

case class UserInfo(uid: String, name: String, team: Int, x: Int, y: Int)
case class NewGame(name: String, n_players: Int, user: UserInfo, ref: ActorRef = null)
case class Game(id: String, name: String, n_players: Int, status: Int, players: List[UserInfo])
case class GamesList(list: List[Game])
case object GamesList
case object GameStatus
case object KillYourself
case object KillMyself
case class UpdatePosition(user: UserInfo)
case class BroadcastUpdatePosition(user: UserInfo)

case class GameStatusBroadcast(game: Game)
case class JoinGame(game: Game, user: UserInfo, ref: ActorRef = null)
case class GameHandler(game: Game, ref: ActorRef = null)
case class LeaveGame(user: UserInfo)
case class NewGameJSON(event: String, name: String, n_players: Int)
case class GameJSON(event: String, game: Game)
case class GamesListResponseJSON(event: String, list: List[Game])
case class GamesListRequestJSON(event: String)
case class JoinGameJSON(event: String, game: Game)
case class LeaveGameJSON(event: String)
case class UpdatePositionJSON(event: String, x: Int, y: Int)
case class BroadcastUpdatePositionJSON(event: String, user: UserInfo)

import play.api.libs.json._

object CommonMessages {
  
  implicit val playerInfoReads = Json.reads[UserInfo]
  implicit val playerInfoWrites = Json.writes[UserInfo]
  
  implicit val leaveGameReads = Json.reads[LeaveGameJSON]
  implicit val leaveGameWrites = Json.writes[LeaveGameJSON]
  
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
  
  implicit val updatePositionReads = Json.reads[UpdatePositionJSON]
  implicit val updatePositionWrites = Json.writes[UpdatePositionJSON]
  
  implicit val broadcastUpdatePositionReads = Json.reads[BroadcastUpdatePositionJSON]
  implicit val broadcastUpdatePositionWrites = Json.writes[BroadcastUpdatePositionJSON]
}
