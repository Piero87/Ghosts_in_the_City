package common

case class NewGame(name: String)
case class Game(id: Long, name: String)
case class GamesList(list: Seq[Game])

case class NewGameJSON(event: String, name: String)
case class GamesListJSON(event: String, list: Seq[Game])

import play.api.libs.json._

object CommonMessages {

  implicit val gameReads = Json.reads[Game]
  implicit val gameWrites = Json.writes[Game]
  
  implicit val newGameReads = Json.reads[NewGameJSON]
  implicit val newGameWrites = Json.writes[NewGameJSON]
  
  implicit val gamesListReads = Json.reads[GamesListJSON]
  implicit val gamesListWrites = Json.writes[GamesListJSON]
  
  //SCRIVI
  //val json = Json.toJson(place)
  //LEGGI
  //val placeResult: JsResult[Place] = json.validate[Place]
}