package backend

import akka.actor._
import play.api.Logger

/*
 * JSSSSSOOOOONNN
 */

object GameManagerBackend {
  
  case class NewGame(name: String)
}
class GameManagerBackend () extends Actor {
  
  import GameManagerBackend._
  
  var managerClient: ActorRef = _
  var game_name = ""
  
  def receive = {
    case NewGame(name) => 
      Logger.info("GameManagerBackend: NewGame request")
      game_name = name
      managerClient = sender()
  }
  
  def newGame () = {
      //...inizializza attori partita
  }
}