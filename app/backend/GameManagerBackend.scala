package backend

import akka.actor._
import play.api.Logger
import common._
import scala.concurrent.duration.Duration;
import java.util.concurrent.TimeUnit;

class GameManagerBackend () extends Actor {
  
  var gameManagerClient: ActorRef = _
  var players: List[UserInfo] = List()
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
    case NewGame(name,n_players,user,ref) =>
      Logger.info("GameManagerBackend: NewGame request")
      game_name = name
      game_n_players = n_players
      game_status = 0
      Logger.info("GMBackend NewGame From: "+ref.toString())
      gameManagerClient = ref
      var p = user
      players = players :+ p
      var g = new Game(game_id,name,n_players,game_status,players)
      sender ! GameHandler(g,self)
    case GameStatus =>
      sender ! Game(game_id,game_name,game_n_players,game_status,players)
    case JoinGame(game,user,ref) =>
      Logger.info("GMBackend richiesta join ricevuta")
      if (players.size < game_n_players) {
        Logger.info("GMBackend richeista join accettata")
        var p = user
        players = players :+ p
        sender ! Game(game_id,game_name,game_n_players,game_status,players)
        //Ora mandiamo il messaggio di update game status a tutti i giocatori (***Dobbiamo evitare di mandarlo a quello che si è
        //appena Joinato?
        //gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players))
        if (players.size == game_n_players)
        {
          //Se è l'ultimo giocatore allora mandiamo il messaggio di star a tutti i giocatori
          game_status = 1
          gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players))
        }
      } else {
        //***Failure message
        Logger.info("GMB: non ci sono più posti per la partita, attaccati al cazzo")
      }
  }
  
  def newGame () = {
      //...inizializza attori partita
  }
}
