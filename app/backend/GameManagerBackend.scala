package backend

import akka.actor._
import play.api.Logger
import common._

class GameManagerBackend () extends Actor {
  
  var gameManagerClient: ActorRef = _
  var game_name = ""
  var game_id = java.util.UUID.randomUUID.toString
  var game_n_players = 0
  var game_status = 0
  
  //ENUM GAME STATUS
  // 0 = waiting_players
  // 1 = started
  // 2 = paused
  // 3 = finished
  
  def receive = {
    case NewGame(name,n_players) =>
      Logger.info("GameManagerBackend: NewGame request")
      game_name = name
      game_n_players = n_players
      game_status = 0
      gameManagerClient = sender()
      gameManagerClient ! Game(game_id,name,n_players,game_status)
    case GameStatus =>
      sender() ! Game(game_id,game_name,game_n_players,game_status)
  }
  
  def newGame () = {
      //...inizializza attori partita
  }
}
