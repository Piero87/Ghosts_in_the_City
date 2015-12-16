package common

import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter

case class _NewGame(name: String)

/**
 * Contains all of the web socket messages and their json formats
 */
sealed abstract class WebMessage

object WebMessage {
  
   /**
   * Sent by the client init New Game
   */
  case class NewGame(
    name:   String
  ) extends WebMessage
  implicit val newGameFormat = Json.format[NewGame]
  
  /**
   * Converts json into WebSocketMessages
   */
  implicit val reads: Reads[WebMessage] = (JsPath \ "new_game").read[NewGame].map(identity[WebMessage])
    
  /**
   * Converts WebSocketMessages into Json
   */
  implicit object writes extends Writes[WebMessage] {
    override def writes(o: WebMessage): JsValue = o match {
      case ng: NewGame       => (JsPath \ "new_game").write(newGameFormat).writes(ng)
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