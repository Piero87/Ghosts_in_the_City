package common

import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter

case class NewGame(name: String)

case class Game(name: String, id: Long)

case class GamesList(gameList: Seq[Game])

/**
 * Contains all of the web socket messages and their json formats
 */
sealed abstract class WebMessage

object WebMessage {
  
   /**
   * Sent by the client init New Game
   */
  case class _NewGame(
    name:   String
  ) extends WebMessage
  implicit val newGameFormat = Json.format[_NewGame]
  
  /**
   * Sent by the client with the game's info
   */
  case class _Game(
    name: String,
    id: Long
  )extends WebMessage
  implicit val gameFormat = Json.format[_Game]
  
  /**
   * Sent by the client with the game's list
   */
  case class _GamesList(
    gamesList: Seq[_Game]
  )extends WebMessage
  implicit val gamesListFormat = Json.format[_GamesList]
  
  
  /**
   * Converts json into WebSocketMessages
   */
  implicit val reads: Reads[WebMessage] = 
    (JsPath \ "new_game").read[_NewGame].map(identity[WebMessage]) orElse 
    (JsPath \ "game").read[_Game].map(identity[WebMessage]) orElse
    (JsPath \ "games_list").read[_GamesList].map(identity[WebMessage])
    
  /**
   * Converts WebSocketMessages into Json
   */
  implicit object writes extends Writes[WebMessage] {
    override def writes(o: WebMessage): JsValue = o match {
      case ng: _NewGame       => (JsPath \ "new_game").write(newGameFormat).writes(ng)
      case g:  _Game          => (JsPath \ "game").write(gameFormat).writes(g)
      case gl:  _GamesList          => (JsPath \ "games_list").write(gamesListFormat).writes(gl)
    }
  }
  
   /**
   * reads and writes WebSocketMessages from/to Json
   */
  implicit val format: Format[WebMessage] = Format(reads,writes)
  
   /**
   * reads and writes WebSocketMessages for the WebSocketActor, uses the format above
   */
  implicit val frameFormat: FrameFormatter[WebMessage] = FrameFormatter.jsonFrame[WebMessage]
}