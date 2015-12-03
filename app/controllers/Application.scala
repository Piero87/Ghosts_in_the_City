package controllers

import play.api._
import play.api.mvc._

//Ciao

class Application extends Controller {
  
  def index = Action {
    Ok(views.html.index())
  }
}
