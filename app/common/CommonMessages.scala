package common

case class PlayerInfo(id: String, name: String, team: String)
case class NewGame(name: String, n_players: Int, user: PlayerInfo)
case class Game(id: String, name: String, n_players: Int, status: Int, players: List[PlayerInfo])
case class GamesList(list: List[Game])
case object GamesList
case object GameStatus
case class GameStatusBroadcast(game: Game)
case class JoinGame(id: String, user: PlayerInfo)

case class NewGameJSON(event: String, name: String, n_players: Int, user: PlayerInfo)
case class GameJSON(event: String, game: Game, source: String)
case class GamesListJSON(event: String, list: List[Game], user: PlayerInfo)
case class JoinGameJSON(event: String, id: String)

import play.api.libs.json._

object CommonMessages {

  implicit val playerInfoReads = Json.reads[PlayerInfo]
  implicit val playerInfoWrites = Json.writes[PlayerInfo]
    
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
