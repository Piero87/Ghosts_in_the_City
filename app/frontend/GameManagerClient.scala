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
  var clientsConnections: List[Tuple2[UserInfo, ActorRef]] = List()
  var game_name = ""
  var game_id = ""
  var game_n_players = 0
  var game_status = 0
  
  def receive = {
    case NewGame(name,n_players,user,ref) =>
      Logger.info("GameManagerClient: NewGame request")
      game_name = name
      game_n_players = n_players
      val origin = sender
      var p = user
      clientsConnections = clientsConnections :+ Tuple2(p,ref)

      val future = backend ? NewGame(name,n_players,user,self)
      future.onSuccess { 
        case GameHandler(game,ref) => 
          Logger.info ("GameManagerClient: Backend Game Manager path: "+sender.path)
          game_id = game.id
          game_status = game.status
          gameManagerBackend = ref
          origin ! GameHandler(game,self)
      }
      future onFailure {
        case e: Exception => Logger.info("******GAME MANAGER CLIENT NEW GAME ERRORE ******")
      }
    case JoinGame(game,user,ref) =>
      Logger.info("GMClient, richiesta JOIN ricevuta")
      if (game_status == 0 && game_id == game.id) {
        Logger.info("GMClient, richiesta JOIN accettata, id: "+game_id)
        var p = user
        clientsConnections = clientsConnections :+ Tuple2(p,ref)
        val origin = sender
        val future = gameManagerBackend ? JoinGame(game,user)
        future.onSuccess { 
          case Game(id,name,n_players, status,players) => 
            Logger.info ("GameManagerClient: Backend Game Manager path: "+sender.path)
            origin ! Game(id,name,n_players, status,players)
        }
        future onFailure {
          case e => Logger.info("****** GAME MANAGER CLIENT JOIN ERRORE ****** =>"+e.getMessage)
        }
      }
    case GameStatusBroadcast(game: Game) =>
      clientsConnections.map {cc =>
        cc._2 forward game
      }
//    case Terminated(a) =>
//      Logger.info("******un ClientConnection è mooooorto*********")
  }
}
