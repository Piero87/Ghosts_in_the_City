package controllers

import play.api._
import play.api.mvc._

//Ciao 4
class Application extends Controller {
  
  def index = Action {
    Ok(views.html.index())
  }
}
