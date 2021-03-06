package controllers


import javax.inject.Inject
import play.api._
import play.api.mvc._
import play.api.Play.current
import akka.actor.Props
import play.api.Logger
import com.typesafe.config.ConfigFactory
import akka.actor._
import frontend.Frontend
import clientactor.ClientConnection
import play.api.libs.json.JsValue


/**
 * Entry point of Ghost in the City server-side application.
 * It recover config parameters from application.conf file and create
 * an istance of [[FontendManager]] actor
 */
class Application extends Controller {
  
  val port = 0
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [frontend]")).
      withFallback(ConfigFactory.load())
  val system = ActorSystem("GhostsSystem", config)
  val frontendManager = system.actorOf(Props[Frontend], name = "frontend")
  
  Logger.info("Application Started")
  
  /**
   * Call application index page 
   */
  def index = Action { implicit request =>
    Logger.info("INDEX LOADED")
    Ok(views.html.index())
    
  }
  
  /**
   * The WebSocket
   */
  def login(name: String, uid: String) = WebSocket.acceptWithActor[JsValue, JsValue] { _ => upstream =>
    ClientConnection.props(name,uid,upstream,frontendManager)
  }
}
