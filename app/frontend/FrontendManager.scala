package frontend

import akka.actor._
import akka.util.Timeout
import common._
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import backend.Backend

class FrontendManager extends Actor {
  
  val logger = new CustomLogger("FrontendManager")
  var backends: List[ActorRef] = List()
  var game_manager_frontends :List[ActorRef] = List()
  
  var backendCounter = 0
  
  
  def receive = {
    case NewGame(name,n_players,player,ref) =>
      logger.log("NewGame request")
      newGame(name,n_players,player,ref)
    case GamesList =>
      gamesList(sender)
    case JoinGame(game,player,ref) =>
      game_manager_frontends.map {gm_fe =>
        gm_fe forward JoinGame(game,player,ref)
      }
    case ResumeGame(game_id, player, ref) =>
       logger.log("ResumeGame request")
       game_manager_frontends.map {gm_fe =>
         gm_fe forward ResumeGame(game_id,player,ref)
       } 
    
    case "BackendRegistration" if !backends.contains(sender()) =>
      logger.log("Backend Received "+sender.path)
      context watch sender()
      backends = backends :+ sender()
    case Terminated(a) =>
      if (a.actorRef == GameManagerClient) {
        logger.log("GMBackend removed")
        game_manager_frontends = game_manager_frontends.filterNot(_ == a)
        
      } else if (a.actorRef == Backend) {
        logger.log("Backend removed")
        backends = backends.filterNot(_ == a)
      }
      
  }
  
  def newGame (name: String,n_players: Int, player: PlayerInfo, ref: ActorRef) = {
    backendCounter += 1
    var b = backends(backendCounter % backends.size)
    val gm_client = context.actorOf(Props(new GameManagerClient(b)), name = name)
    context watch gm_client
    game_manager_frontends = game_manager_frontends :+ gm_client
    logger.log("Backend selected and Actor created, forwarding message...")
    gm_client forward NewGame(name,n_players, player,ref)
  }
  
  def gamesList (origin: ActorRef) = {
    
    //il timeout è relativa al tempo di attesa della risposta all ask(?)
    val taskFutures: List[Future[List[Game]]] = backends map { be =>
        implicit val timeout = Timeout(5 seconds)
        (be ? GamesList).mapTo[List[Game]]
    }
    implicit val ec = context.dispatcher
    val searchFuture = Future sequence taskFutures
    
     searchFuture.onSuccess {
      case results: List[List[Game]] => origin ! GamesList(results.flatten)
    }
  }
}
