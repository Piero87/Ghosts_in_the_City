package frontend

import akka.actor._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.util.{Failure, Success}
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
  
  val logger = new CustomLogger("GameManagerClient")
  var gameManagerBackend: ActorRef = _
  var clientsConnections: MutableList[Tuple2[PlayerInfo, ActorRef]] = MutableList()
  var game_name = ""
  var game_id = ""
  var game_n_players = 0
  var game_status = StatusGame.WAITING
  
  def receive = {
    case NewGame(name,n_players,player,ref) =>
      logger.log("NewGame request (" + player.name + ")")
      game_name = name
      game_n_players = n_players
      val origin = sender
      var p = player
      clientsConnections = clientsConnections :+ Tuple2(p,ref)

      val future = backend ? NewGame(name,n_players,player,self)
      future.onSuccess { 
        case GameHandler(game,ref) => 
          logger.log("GameManagerBackend path: "+sender.path)
          game_id = game.id
          game_status = game.status
          gameManagerBackend = ref
          origin ! GameHandler(game,self)
      }
      future onFailure {
        case e: Exception => logger.log("NEW GAME ERROR: " + e.getMessage + " FROM " + sender.path)
      }
    case JoinGame(game,player,ref) =>
      logger.log("JoinGame request, GameID: " + game_id + " (" + player.name + ")")
      if (game_status == 0 && game_id == game.id) {
        logger.log("JoinGame request ACCEPTED, GameID: " + game_id + " (" + player.name + ")")
        // Per sicurezza ci salviamo i dati del client connection che ci ha mandato la richiesta di join
        var p = player
        var ccref = ref
        val origin = sender
        val future = gameManagerBackend ? JoinGame(game,player)
        future.onSuccess { 
          case Game(id,name,n_players, status,players,ghosts,treasures) => 
            logger.log("GameManagerBackend path: "+sender.path)
            clientsConnections = clientsConnections :+ Tuple2(p,ccref)
            var g = new Game(id,name,n_players,status,players,ghosts,treasures)
            origin ! GameHandler(g,self)
        }
        future onFailure {
          case e => logger.log("JOIN GAME ERROR: " + e.getMessage + " FROM " + sender.path)
        }
      }
    case ResumeGame(gameid,player,ref) =>
      if (game_id == gameid) {
        logger.log("ResumeGame request, GameID: " + gameid + " (" + player.name + ")")
        var p = player
        var ccref = ref
        val origin = sender
        val future = gameManagerBackend ? ResumeGame(gameid,player,ref)
        future.onSuccess { 
          case Game(id,name,n_players, status,players,ghosts,treasures) => 
            for (i <- 0 to clientsConnections.size-1) {
              if (clientsConnections(i)._1.uid == player.uid) {
                clientsConnections(i) = clientsConnections(i).copy(_2 = ref)
              }
            }
            var g = new Game(id,name,n_players,status,players,ghosts,treasures)
            origin ! GameHandler(g,self)
        }
        future onFailure {
          case e => logger.log("RESUME GAME ERROR: " + e.getMessage + " FROM " + sender.path)
        } 
      }
      
    case GameStatusBroadcast(game: Game) =>
      game_status = game.status
      clientsConnections.map {cc =>
        cc._2 forward GameStatusBroadcast(game)
      }
      
    case PauseGame(player:PlayerInfo) =>
      logger.log("PauseGame request (" + player.name + ")")
      gameManagerBackend ! PauseGame(player)
    
    case LeaveGame(player: PlayerInfo) =>
      logger.log("LeaveGame request (" + player.name + ")")
      val future = gameManagerBackend ? LeaveGame(player)
      future.onSuccess { 
        case Success =>
          clientsConnections = clientsConnections.filterNot(elm => elm._1.uid == player.uid)       
      }
      future onFailure {
        case e: Exception => logger.log("LEAVE GAME ERROR: " + e.getMessage + " FROM " + sender.path)
      }
    
    case KillYourself => 
       logger.log("Goodbye cruel cluster!")
       game_status = StatusGame.FINISHED
       if (clientsConnections.size > 0) {
         var g = new Game(game_id,game_name,game_n_players,game_status,MutableList(),MutableList(),MutableList())
         self ! GameStatusBroadcast(g)
       }
       sender ! KillMyself
       self ! PoisonPill
    
    case UpdatePosition(player) =>
      gameManagerBackend ! UpdatePosition(player)
    
    case BroadcastUpdatePosition(player) =>
      clientsConnections.map {cc =>
        if (cc._1.uid != player.uid) cc._2 forward BroadcastUpdatePosition(player)
      }
    
    case BroadcastGhostsPositions(ghosts) =>
      clientsConnections.map {cc =>
        cc._2 forward BroadcastGhostsPositions(ghosts)
      }
    case SetTrapRequest(player) =>
      gameManagerBackend ! SetTrapRequest(player)
    
    case HitPlayerRequest(p_uid) =>
      gameManagerBackend ! HitPlayerRequest(p_uid)
    case BroadcastNewTrap(trap) =>
      clientsConnections.map {cc =>
        cc._2 forward BroadcastNewTrap(trap)
      }
    case BroadcastTrapActivated(trap) =>
      clientsConnections.map {cc =>
        cc._2 forward BroadcastTrapActivated(trap)
      }
    case BroadcastRemoveTrap(trap) =>
      clientsConnections.map {cc =>
        cc._2 forward BroadcastRemoveTrap(trap)
      }
    case UpdatePlayerInfo(player) =>
      clientsConnections.map {cc =>
        if (cc._1.uid == player.uid) cc._2 forward UpdatePlayerInfo(player)
      }
    case MessageCode(uid,code,option) =>
      clientsConnections.map {cc =>
        if (cc._1.uid == uid) cc._2 forward MessageCode(uid,code,option)
      }
    case OpenTreasureRequest(uid) =>
      gameManagerBackend ! OpenTreasureRequest(uid)
    case BroadcastUpdateTreasure(treasures) =>
      clientsConnections.map {cc =>
        cc._2 forward BroadcastUpdateTreasure(treasures)
      }
    case BroadcastVictoryResponse(team, players) =>
      clientsConnections.map {cc =>
        cc._2 forward BroadcastVictoryResponse(team, players)
      }
        
  }
}
