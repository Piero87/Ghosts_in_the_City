package frontend

import akka.actor._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.util.{Failure, Success}
import play.api.Logger
import common._

object GameManagerClient {
  
  def props(backend: ActorRef) = Props(new GameManagerClient(backend))
}
class GameManagerClient (backend: ActorRef) extends Actor {
  
  import GameManagerClient._
  
  var gameManagerBackend: ActorRef = _
  var clientConnection: ActorRef = _
  var game_name = ""
  var game_id = java.util.UUID.randomUUID.toString
  var game_n_players = 0
  
  def receive = {
    case NewGame(name,n_players) =>
      Logger.info("GameManagerClient: NewGame request")
      game_name = name
      game_n_players = n_players
      clientConnection = sender()
      implicit val timeout = Timeout(5 seconds)
      implicit val ec = context.dispatcher
      val future = backend ? NewGame(name,n_players)
      future.onSuccess { 
        case Game(id,name,n_players, status) => 
          Logger.info ("GameManagerClient: Backend Game Manager path: "+sender.path)
          game_id = id
          gameManagerBackend = sender
          clientConnection ! Game(id,name,n_players, status)
      }
      future onFailure {
        case e: Exception => Logger.info("****** ERRORE ******")
      }
  }
}
