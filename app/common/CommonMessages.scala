package common

case class NewGame(name: String)
case class Game(id: Long, name: String)
case class GamesList(games_list: Seq[Game])

import play.api.libs.json._

object CommonMessages {

  implicit val newGameReads = new Reads[NewGame] {
    def reads(json: JsValue): JsResult[NewGame] = JsSuccess(new NewGame (
      (json \ "name").as[String]
    ))
  }
  
  implicit val newGameWrites = new Writes[NewGame] {
    def writes(newGame: NewGame) = Json.obj(
      "name" -> newGame.name
    )
  }
  
  implicit val gameReads = new Reads[Game] {
    def reads(json: JsValue): JsResult[Game] = JsSuccess(new Game (
      (json \ "id").as[Long],
      (json \ "name").as[String]
    ))
  }
  
  implicit val gameWrites = new Writes[Game] {
    def writes(game: Game) = Json.obj(
      "id" -> game.id,
      "name" -> game.name
    )
  }
  
  implicit val gamesListReads = new Reads[GamesList] {
    def reads(json: JsValue): JsResult[GamesList] = JsSuccess(new GamesList (
      (json \ "games_list").as[Seq[Game]]
      )
    )
  }
  
  implicit val gamesListWrites = new Writes[GamesList] {
    def writes(gamesList: GamesList) = Json.obj(
      "games_list" -> gamesList.games_list)
  }
  
  //SCRIVI
  //val json = Json.toJson(place)
  //LEGGI
  //val placeResult: JsResult[Place] = json.validate[Place]
}