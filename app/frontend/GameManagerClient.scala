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

  implicit val timeout = Timeout(5 seconds)
  implicit val ec = context.dispatcher
  
  var gameManagerBackend: ActorRef = _
  var clientsConnections: List[Tuple2[PlayerInfo, ActorRef]] = List()
  var game_name = ""
  var game_id = java.util.UUID.randomUUID.toString
  var game_n_players = 0
  var game_status = 0
  
  def receive = {
    case NewGame(name,n_players,user) =>
      Logger.info("GameManagerClient: NewGame request")
      game_name = name
      game_n_players = n_players
      var client_creator = sender
      var p = user
      clientsConnections = clientsConnections :+ Tuple2(p,sender)
//      context watch sender
      val future = backend ? NewGame(name,n_players,user)
      future.onSuccess { 
        case Game(id,name,n_players, status,players) => 
          Logger.info ("GameManagerClient: Backend Game Manager path: "+sender.path)
          game_id = id
          game_status = status
          gameManagerBackend = sender
          client_creator ! Game(id,name,n_players, status,players)
      }
      future onFailure {
        case e: Exception => Logger.info("****** ERRORE ******")
      }
    case JoinGame(id,user) =>
      if (game_status == 0 && game_id == id) {
        var p = user
        clientsConnections = clientsConnections :+ Tuple2(p,sender)
        var origin = sender
        val future = gameManagerBackend ? JoinGame(id,user)
        future.onSuccess { 
          case Game(id,name,n_players, status,players) => 
            Logger.info ("GameManagerClient: Backend Game Manager path: "+sender.path)
            origin ! Game(id,name,n_players, status,players)
        }
        future onFailure {
          case e: Exception => Logger.info("****** ERRORE ******")
        }
      }
    case GameStatusBroadcast(game: Game) =>
      for (i <- 0 to clientsConnections.size) {
        clientsConnections(i)._2 forward game
      }
//    case Terminated(a) =>
//      Logger.info("******un ClientConnection è mooooorto*********")
  }
}
