package backend

import akka.actor._
import play.api.Logger
import common._

class GameManagerBackend () extends Actor {
  
  var gameManagerClient: ActorRef = _
  var players: List[PlayerInfo] = List()
  var game_name = ""
  var game_id = java.util.UUID.randomUUID.toString
  var game_n_players = 0
  var game_status = 0
  
  //ENUM GAME STATUS
  // 0 = waiting
  // 1 = started
  // 2 = paused
  // 3 = finished
  
  def receive = {
    case NewGame(name,n_players,uuid_user,username) =>
      Logger.info("GameManagerBackend: NewGame request")
      game_name = name
      game_n_players = n_players
      game_status = 0
      gameManagerClient = sender()
      var p = new PlayerInfo(uuid_user,username,"")
      players = players :+ p
      gameManagerClient ! Game(game_id,name,n_players,game_status,players)
    case GameStatus =>
      sender() ! Game(game_id,game_name,game_n_players,game_status,players)
    case JoinGame(id,username,uuid) =>
      if (players.size < game_n_players) {
        var p = new PlayerInfo(uuid,username,"")
        players = players :+ p
        gameManagerClient ! Game(game_id,game_name,game_n_players,game_status,players)
        //Ora mandiamo il messaggio di update game status a tutti i giocatori (***Dobbiamo evitare di mandarlo a quello che si è
        //appena Joinato?
        gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players))
        if (players.size == game_n_players)
        {
          //Se è l'ultimo giocatore allora mandiamo il messaggio di star a tutti i giocatori
        }
      } else {
        //***Failure message
      }
  }
  
  def newGame () = {
      //...inizializza attori partita
  }
}
