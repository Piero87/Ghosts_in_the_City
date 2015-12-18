package frontend

import akka.actor._
import play.api.Logger
import akka.util.Timeout
import common._

class FrontendManager extends Actor {
  
  var backends = IndexedSeq.empty[ActorRef]
  var game_manager_frontends = IndexedSeq.empty[ActorRef]
  
  var backendCounter = 0
  
  def receive = {
//    case EnterMatch(username) if backends.isEmpty =>
//      sender() ! JobFailed("Service unavailable, try again later", job)

    case NewGame(name,n_players) =>
      Logger.info("FrontendManager: NewGame request")
      newGame(name,n_players)
      
    case "BackendRegistration" if !backends.contains(sender()) =>
      Logger.info("Backend Received "+sender.path)
      context watch sender()
      backends = backends :+ sender()
    case Terminated(a) =>
      backends = backends.filterNot(_ == a)
  }
  
  def newGame (name: String,n_players: Int) = {
    backendCounter += 1
    var b = backends(backendCounter % backends.size)
    val gm_client = context.actorOf(Props(new GameManagerClient(b)), name = name)
    game_manager_frontends = game_manager_frontends :+ gm_client
    Logger.info("FrontendManager: Backend selected and Actor created, forward message...")
    gm_client forward NewGame(name,n_players)
    
  }
}