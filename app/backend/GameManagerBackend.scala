package backend

import akka.actor._
import play.api.Logger
import common._

class GameManagerBackend () extends Actor {
  
  var gameManagerClient: ActorRef = _
  var game_name = ""
  var game_id = java.util.UUID.randomUUID.toString
  
  def receive = {
    case NewGame(name) =>
      Logger.info("GameManagerBackend: NewGame request")
      game_name = name
      gameManagerClient = sender()
      sender() ! self
  }
  
  def newGame () = {
      //...inizializza attori partita
  }
}