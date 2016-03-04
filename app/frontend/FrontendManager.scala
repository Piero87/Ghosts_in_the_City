package frontend

import akka.actor._
import akka.util.Timeout
import common._
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import backend.Backend

/**
 * Actor FrontendManager implementation class.
 * Its akka.actor.Props object is defined in Application class because
 * FrontendManager actor is a child of system actor
 */
class FrontendManager extends Actor {
  
  val logger = new CustomLogger("FrontendManager")
  var backends: List[ActorRef] = List()
  var game_manager_frontends :List[ActorRef] = List()
  
  var backendCounter = 0
  
  /**
   * Receive method.
   * It helds all the messages that could be sent to the FrontendManager actor from ClientConnection or server
   */
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
    /* FrontendManager have to take trace of all active backends. 
     * That procedure is made by a registration request from the backend. 
		 */
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
  
  /**
   * New Game method.
   * It forwards the new game requests to a backend in the backend list selected through the round-robin algorithm
   */
  def newGame (name: String,n_players: Int, player: PlayerInfo, ref: ActorRef) = {
    backendCounter += 1
    var b = backends(backendCounter % backends.size)
    val gm_client = context.actorOf(Props(new GameManagerClient(b)), name = name)
    // We monitor the gm_client lifecycle to be notified when it stops and to receive the Terminated message.
    // We want an actor to be notified when another actor dies.
    context watch gm_client
    game_manager_frontends = game_manager_frontends :+ gm_client
    logger.log("Backend selected and Actor created, forwarding message...")
    gm_client forward NewGame(name,n_players, player,ref)
  }
  
  /**
   * Game List method.
   * It ask to all backends the games list that they held.
   */
  def gamesList (origin: ActorRef) = {
    
    // Timeout refers to the answer waiting time to the ask command( ? )
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
