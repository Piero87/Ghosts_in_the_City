package frontend

import akka.actor._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.util.{Failure, Success}
import play.api.Logger
import common._
import akka.actor.PoisonPill
import scala.collection.mutable.MutableList

object GameManagerClient {
  
  def props(backend: ActorRef) = Props(new GameManagerClient(backend))
}
class GameManagerClient (backend: ActorRef) extends Actor {
  
  import GameManagerClient._

  implicit val timeout = Timeout(5 seconds)
  implicit val ec = context.dispatcher
  
  var gameManagerBackend: ActorRef = _
  var clientsConnections: MutableList[Tuple2[UserInfo, ActorRef]] = MutableList()
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
          case Game(id,name,n_players, status,players,ghosts,treasures) => 
            Logger.info ("GameManagerClient: Backend Game Manager path: "+sender.path)
            clientsConnections = clientsConnections :+ Tuple2(p,ccref)
            var g = new Game(id,name,n_players,status,players,ghosts,treasures)
            origin ! GameHandler(g,self)
        }
        future onFailure {
          case e => Logger.info("****** GAME MANAGER CLIENT JOIN ERRORE ****** =>"+e.getMessage)
        }
      }
    case ResumeGame(gameid,user,ref) =>
      Logger.info("GMClient, richiesta RESUME ricevuta")
      if (game_id == gameid) {
        var p = user
        var ccref = ref
        val origin = sender
        val future = gameManagerBackend ? ResumeGame(gameid,user,ref)
        future.onSuccess { 
          case Game(id,name,n_players, status,players,ghosts,treasures) => 
            for (i <- 0 to clientsConnections.size-1) {
              if (clientsConnections(i)._1.uid == user.uid) {
                clientsConnections(i) = clientsConnections(i).copy(_2 = ref)
              }
            }
            var g = new Game(id,name,n_players,status,players,ghosts,treasures)
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
      
    case PauseGame(user:UserInfo) =>
      Logger.info("GMClient: PauseGame Request")
      gameManagerBackend ! PauseGame(user)
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
       if (clientsConnections.size > 0) {
         var g = new Game(game_id,game_name,game_n_players,game_status,MutableList(),MutableList(),MutableList())
         self ! GameStatusBroadcast(g)
       }
       sender ! KillMyself
       self ! PoisonPill
    case UpdatePosition(user) =>
      gameManagerBackend ! UpdatePosition(user)
    case BroadcastUpdatePosition(user) =>
      clientsConnections.map {cc =>
        if (cc._1.uid != user.uid) cc._2 forward BroadcastUpdatePosition(user)
      }
    case BroadcastGhostsPositions(ghosts) =>
      clientsConnections.map {cc =>
        cc._2 forward BroadcastGhostsPositions(ghosts)
      }
        
  }
}
