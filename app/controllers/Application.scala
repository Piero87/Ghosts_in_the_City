package controllers


import javax.inject.Inject
import play.api._
import play.api.mvc._
import play.api.Play.current
import akka.actor.Props
import play.api.Logger
import actors.ClientConnection
import play.api.libs.json.JsValue

class Application extends Controller {
  
  def index = Action { implicit request =>
    Logger.debug("INDEX STARTED")
    Ok(views.html.index())
    
  }
  
  /**
   * The WebSocket
   */
  def stream(email: String) = WebSocket.acceptWithActor[String, String] { _ => out =>
    ClientConnection.props(email,out)
  }
}
