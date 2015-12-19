package common

case class NewGame(name: String, n_players: Int, uuid_user: String, user_name: String)
case class PlayerInfo(uuid: String, username: String, team: String)
case class Game(id: String, name: String, n_players: Int, status: Int, players: List[PlayerInfo])
case class GamesList(list: List[Game])
case object GamesList
case object GameStatus
case class GameStatusBroadcast(game: Game)
case class JoinGame(id: String, username: String, uuid: String)




case class NewGameJSON(event: String, name: String, n_players: Int, source: String)
case class GameJSON(event: String, game: Game, source: String)
case class GamesListJSON(event: String, list: List[Game], source: String)
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
  

  
  //SCRIVI
  //val json = Json.toJson(place)
  //LEGGI
  //val placeResult: JsResult[Place] = json.validate[Place]
}
