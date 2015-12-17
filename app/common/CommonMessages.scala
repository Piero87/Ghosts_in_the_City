package common

case class NewGame(name: String)
case class NewGameJSON(event: String, name: String)
case class Game(event: String, id: Long, name: String)
case class GamesList(event: String, list: Seq[Game])

import play.api.libs.json._

object CommonMessages {

  implicit val newGameReads = new Reads[NewGameJSON] {
    def reads(json: JsValue): JsResult[NewGameJSON] = JsSuccess(new NewGameJSON (
      (json \ "event").as[String],
      (json \ "name").as[String]
    ))
  }
  
  implicit val newGameWrites = new Writes[NewGameJSON] {
    def writes(newGame: NewGameJSON) = Json.obj(
      "event" -> "new_game",  
      "name" -> newGame.name
    )
  }
  
  implicit val gameReads = new Reads[Game] {
    def reads(json: JsValue): JsResult[Game] = JsSuccess(new Game (
      (json \ "event").as[String],
      (json \ "id").as[Long],
      (json \ "name").as[String]
    ))
  }
  
  implicit val gameWrites = new Writes[Game] {
    def writes(game: Game) = Json.obj(
      "event" -> "game",
      "id" -> game.id,
      "name" -> game.name
    )
  }
  
  implicit val gamesListReads = new Reads[GamesList] {
    def reads(json: JsValue): JsResult[GamesList] = JsSuccess(new GamesList (
        (json \ "event").as[String],
        (json \ "list").as[Seq[Game]]
      )
    )
  }
  
  implicit val gamesListWrites = new Writes[GamesList] {
    def writes(gamesList: GamesList) = Json.obj(
      "event" -> "games_list",
      "list" -> gamesList.list
    )
  }
  
  //SCRIVI
  //val json = Json.toJson(place)
  //LEGGI
  //val placeResult: JsResult[Place] = json.validate[Place]
}