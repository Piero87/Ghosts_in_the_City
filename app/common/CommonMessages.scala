package common

import akka.actor._

case class UserInfo(uid: String, name: String, team: String)
case class NewGame(name: String, n_players: Int, user: UserInfo, ref: ActorRef = null)
case class Game(id: String, name: String, n_players: Int, status: Int, players: List[UserInfo])
case class GamesList(list: List[Game])
case object GamesList
case object GameStatus
case class GameStatusBroadcast(game: Game)
case class JoinGame(game: Game, user: UserInfo, ref: ActorRef = null)
case class GameHandler(game: Game, ref: ActorRef = null)

case class NewGameJSON(event: String, name: String, n_players: Int, user: UserInfo)
case class GameJSON(event: String, game: Game, user: UserInfo)
case class GamesListJSON(event: String, list: List[Game], user: UserInfo)
case class JoinGameJSON(event: String, game: Game,user: UserInfo)

import play.api.libs.json._

object CommonMessages {

  implicit val playerInfoReads = Json.reads[UserInfo]
  implicit val playerInfoWrites = Json.writes[UserInfo]
    
  implicit val gameReads = Json.reads[Game]
  implicit val gameWrites = Json.writes[Game]
  
  implicit val gameJSONReads = Json.reads[GameJSON]
  implicit val gameJSONWrites = Json.writes[GameJSON]
  
  implicit val newGameReads = Json.reads[NewGameJSON]
  implicit val newGameWrites = Json.writes[NewGameJSON]
  
  implicit val gamesListReads = Json.reads[GamesListJSON]
  implicit val gamesListWrites = Json.writes[GamesListJSON]
  
  implicit val joinGameReads = Json.reads[JoinGameJSON]
  implicit val joinGameWrites = Json.writes[JoinGameJSON]
}
