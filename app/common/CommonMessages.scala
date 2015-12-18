package common

case class NewGame(name: String, n_players: Int)
case class Game(id: String, name: String, n_players: Int, status: Int)
case class GamesList(list: List[Game])
case object GamesList
case object GameStatus

case class NewGameJSON(event: String, name: String, n_players: Int, source: String)
case class GameJSON(event: String, game: Game, source: String)
case class GamesListJSON(event: String, list: List[Game], source: String)

import play.api.libs.json._

object CommonMessages {

  implicit val gameReads = Json.reads[Game]
  implicit val gameWrites = Json.writes[Game]
  
  implicit val gameJSONReads = Json.reads[GameJSON]
  implicit val gameJSONWrites = Json.writes[GameJSON]
  
  implicit val newGameReads = Json.reads[NewGameJSON]
  implicit val newGameWrites = Json.writes[NewGameJSON]
  
  implicit val gamesListReads = Json.reads[GamesListJSON]
  implicit val gamesListWrites = Json.writes[GamesListJSON]
  
  //SCRIVI
  //val json = Json.toJson(place)
  //LEGGI
  //val placeResult: JsResult[Place] = json.validate[Place]
}
