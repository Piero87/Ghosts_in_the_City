package backend

import akka.actor._
import play.api.Logger
import common._

class GameManagerBackend () extends Actor {
  
  var gameManagerClient: ActorRef = _
  var game_name = ""
  var game_id = java.util.UUID.randomUUID.toString
  var game_n_players = 0
  
  def receive = {
    case NewGame(name,n_players) =>
      Logger.info("GameManagerBackend: NewGame request")
      game_name = name
      game_n_players = n_players
      gameManagerClient = sender()
      gameManagerClient ! Game(game_id,name,n_players)
  }
  
  def newGame () = {
      //...inizializza attori partita
  }
}