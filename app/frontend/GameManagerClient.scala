package frontend

import akka.actor._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.util.{Failure, Success}
import play.api.Logger
import common._
import akka.actor.PoisonPill

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
  var game_status = StatusGame.WAITING
  
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
        // Per sicurezza ci salviamo i dati del client connection che ci ha mandato la richiesta di join
        var p = user
        var ccref = ref
        val origin = sender
        val future = gameManagerBackend ? JoinGame(game,user)
        future.onSuccess { 
          case Game(id,name,n_players, status,players) => 
            Logger.info ("GameManagerClient: Backend Game Manager path: "+sender.path)
            clientsConnections = clientsConnections :+ Tuple2(p,ccref)
            var g = new Game(id,name,n_players,status,players)
            origin ! GameHandler(g,self)
        }
        future onFailure {
          case e => Logger.info("****** GAME MANAGER CLIENT JOIN ERRORE ****** =>"+e.getMessage)
        }
      }
    case GameStatusBroadcast(game: Game) =>
      game_status = game.status
      clientsConnections.map {cc =>
        cc._2 forward GameStatusBroadcast(game)
      }
    case LeaveGame(user: UserInfo) =>
      Logger.info("GMClient: LeaveGame Request")
      val future = gameManagerBackend ? LeaveGame(user)
      future.onSuccess { 
        case Success =>
          clientsConnections = clientsConnections.filterNot(elm => elm._1.uid == user.uid)       
      }
      future onFailure {
        case e: Exception => Logger.info("******GAME MANAGER BACKEND KILL ERROR ******")
      }
    case KillYourself => 
       Logger.info ("GameManagerBackend: GMClient will die")
       game_status = StatusGame.FINISHED
       var g = new Game(game_id,game_name,game_n_players,game_status,List())
       self ! GameStatusBroadcast(g)
       sender ! KillMyself
       self ! PoisonPill
        
  }
}
