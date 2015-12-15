package controllers


import javax.inject.Inject
import play.api._
import play.api.mvc._
import play.api.Play.current
import akka.actor.Props
import play.api.Logger
import com.typesafe.config.ConfigFactory
import akka.actor._
import frontend.FrontendManager
import actors.ClientConnection
import actors.ClientConnection.ClientEvent



class Application extends Controller {
  
  val port = 0
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [frontend]")).
      withFallback(ConfigFactory.load())
  val system = ActorSystem("TreasuresSystem", config)
  val frontendManager = system.actorOf(Props[FrontendManager], name = "frontend")
  
  Logger.info("Application Started")
  
  def index = Action { implicit request =>
    Logger.info("INDEX STARTED")
    Ok(views.html.index())
    
  }
  
  /**
   * The WebSocket
   */
  def stream(username: String) = WebSocket.acceptWithActor[ClientEvent, ClientEvent] { _ => upstream =>
    ClientConnection.props(username,upstream,frontendManager)
  }
//  
//  def createRecoverFrontendService () {
//    actorSystem.actorOf(Props[RecoverFrontend], "recoverFrontend")
//  }
}
