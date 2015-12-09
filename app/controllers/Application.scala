package controllers


import javax.inject.Inject

import play.api._
import play.api.mvc._
import play.api.Play.current
import akka.actor.Props

import actors.ClientConnection
import actors.ClientConnection.ClientEvent

class Application  @Inject() (
    clientConnectionFactory: ClientConnection.Factory) extends Controller {
  
  def index = Action {
    Ok(views.html.index())
  }
  
  /**
   * The WebSocket
   */
  def stream(email: String) = WebSocket.acceptWithActor[ClientEvent, ClientEvent] { _ => upstream =>
    Props(clientConnectionFactory(email, upstream))
  }
}
