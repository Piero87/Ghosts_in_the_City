package frontend

import akka.actor._
import play.api.Logger
import akka.util.Timeout
import common._
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}

class FrontendManager extends Actor {
  
  var backends: List[ActorRef] = List()
  var game_manager_frontends = IndexedSeq.empty[ActorRef]
  
  var backendCounter = 0
  
  def receive = {
//    case EnterMatch(username) if backends.isEmpty =>
//      sender() ! JobFailed("Service unavailable, try again later", job)

    case NewGame(name,n_players,uuid_user,username) =>
      Logger.info("FrontendManager: NewGame request")
      newGame(name,n_players,uuid_user,username)
    case GamesList =>
      gamesList(sender)
    case JoinGame(id,username,uuid) =>
      for (i <- 0 to game_manager_frontends.size) {
        game_manager_frontends(i) forward JoinGame(id,username,uuid)
      }
    
    case "BackendRegistration" if !backends.contains(sender()) =>
      Logger.info("Backend Received "+sender.path)
      context watch sender()
      backends = backends :+ sender()
    case Terminated(a) =>
      backends = backends.filterNot(_ == a)
  }
  
  def newGame (name: String,n_players: Int, uuid_user: String, username: String) = {
    backendCounter += 1
    var b = backends(backendCounter % backends.size)
    val gm_client = context.actorOf(Props(new GameManagerClient(b)), name = name)
    game_manager_frontends = game_manager_frontends :+ gm_client
    Logger.info("FrontendManager: Backend selected and Actor created, forward message...")
    gm_client forward NewGame(name,n_players,uuid_user,username)
  }
  
  def gamesList (origin: ActorRef) = {
    
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
