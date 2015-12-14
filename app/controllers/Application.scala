package controllers


import javax.inject.Inject
import play.api._
import play.api.mvc._
import play.api.Play.current
import akka.actor.Props
import play.api.Logger
import com.typesafe.config.ConfigFactory
import akka.actor._
import actors.RecoverFrontend
import actors.Frontend
import actors.ClientConnection
import actors.ClientConnection.ClientEvent



class Application extends Controller {
  
  val port = 0
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [frontend]")).
      withFallback(ConfigFactory.load())
  val system = ActorSystem("TreasuresSystem", config)
  val frontend = system.actorOf(Props[Frontend], name = "frontend")
  
  Logger.info("Application Started")
  
  def index = Action { implicit request =>
    Logger.info("INDEX STARTED")
    Ok(views.html.index())
    
  }
  
  /**
   * The WebSocket
   */
  def stream(username: String) = WebSocket.acceptWithActor[ClientEvent, ClientEvent] { _ => upstream =>
    ClientConnection.props(username,upstream,frontend)
  }
//  
//  def createRecoverFrontendService () {
//    actorSystem.actorOf(Props[RecoverFrontend], "recoverFrontend")
//  }
}
