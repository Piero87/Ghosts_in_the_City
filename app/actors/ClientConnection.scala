package actors

import akka.actor._
import play.api.Logger
import play.extras.geojson.LatLng
import play.extras.geojson.Point
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.util.{Failure, Success}
import common._
import common.WebMessage._

object ClientConnection {
  
  def props(username: String, upstream: ActorRef, frontend: ActorRef) = Props(new ClientConnection(username,upstream,frontend))
  
}

class ClientConnection(username: String, upstream: ActorRef,frontendManager: ActorRef) extends Actor {
  
  var gameManagerClient: ActorRef = _
  
  def receive = {
    case _NewGame(name) => 
      Logger.info("ClientConnection: NewGame received")
      implicit val timeout = Timeout(5 seconds)
      implicit val ec = context.dispatcher
      val future = frontendManager ? NewGame(name.replaceAll(" ", "_"))
      future.onSuccess { 
        case result: ActorRef => 
          Logger.info ("ClientConnection NewGame result: "+result.path)
          gameManagerClient = result
      }
      future onFailure {
        case e: Exception => Logger.info("****** ERRORE ******")
      }
  }
}

      /*
      implicit val timeout = Timeout(5 seconds)
      implicit val ec = context.dispatcher
      frontendManager ? NewGame(name) andThen {
        case Success(_) => 
          managerClient = sender()
          upstream ! NewGame(name)
        case Failure(_) => Logger.info("Errore ClientConnection Creazione Partita")
      }
      */
      //managerClient = frontendManager ? NewGame(name) //Qui mi aspetterò l'actorRef di ManagerClient con cui parlerò 
