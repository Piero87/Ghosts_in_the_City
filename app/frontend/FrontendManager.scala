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
  var game_manager_frontends :List[ActorRef] = List()
  
  var backendCounter = 0
  
  def receive = {
//    case EnterMatch(username) if backends.isEmpty =>
//      sender() ! JobFailed("Service unavailable, try again later", job)

    case NewGame(name,n_players,user,ref) =>
      Logger.info("FrontendManager: NewGame request")
      newGame(name,n_players,user,ref)
    case GamesList =>
      gamesList(sender)
    case JoinGame(game,user,ref) =>
      game_manager_frontends.map {gm_fe =>
        gm_fe forward JoinGame(game,user,ref)
      }
    
    case "BackendRegistration" if !backends.contains(sender()) =>
      Logger.info("Backend Received "+sender.path)
      context watch sender()
      backends = backends :+ sender()
    case Terminated(a) =>
      backends = backends.filterNot(_ == a)
  }
  
  def newGame (name: String,n_players: Int, user: UserInfo, ref: ActorRef) = {
    backendCounter += 1
    var b = backends(backendCounter % backends.size)
    val gm_client = context.actorOf(Props(new GameManagerClient(b)), name = name)
    game_manager_frontends = game_manager_frontends :+ gm_client
    Logger.info("FrontendManager: Backend selected and Actor created, forward message...")
    gm_client forward NewGame(name,n_players, user,ref)
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
