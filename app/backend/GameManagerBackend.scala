package backend

import akka.actor._
import play.api.Logger
import common._

/*
 * JSSSSSOOOOONNN
 */

class GameManagerBackend () extends Actor {
  
  var gameManagerClient: ActorRef = _
  var game_name = ""
  
  def receive = {
    case _NewGame(name) =>
      Logger.info("GameManagerBackend: NewGame request")
      game_name = name
      gameManagerClient = sender()
      sender() ! self
  }
  
  def newGame () = {
      //...inizializza attori partita
  }
}