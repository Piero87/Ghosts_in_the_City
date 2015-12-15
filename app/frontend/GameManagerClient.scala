package frontend

import akka.actor._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.util.{Failure, Success}
import play.api.Logger

/*
 * JSONNNNNNNN
 */

object GameManagerClient {
  
  case class NewGame(name: String)
  
  def props(backend: ActorRef) = Props(new GameManagerClient(backend))
}
class GameManagerClient (backend: ActorRef) extends Actor {
  
  import GameManagerClient._
  
  var managerBackend: ActorRef = _
  var clientConnection: ActorRef = _
  var game_name = ""
  
  def receive = {
    case NewGame(name) =>
      Logger.info("GameManagerClient: NewGame request")
      game_name = name
      clientConnection = sender()
      sender() ! self
      implicit val timeout = Timeout(5 seconds)
      implicit val ec = context.dispatcher
      backend ? NewGame(name) andThen {
        case Success(_) => managerBackend = sender()
        case Failure(_) => Logger.info("Errore ClientConnection Creazione Partita")
      }
  }
}