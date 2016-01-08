package backend.actors

import akka.actor.{ ActorRef, Actor }
import akka.actor.Props
import play.api.Logger
import play.api.libs.json._
import common._

object Player {

  /**
   * Actor Props 
   */
  def props(area : Polygon, position: Point): Props = Props(new Player(area, position))
  
}

class Player(area : Polygon, position: Point) extends Actor{
  
  def receive = {
   case "test" => 
      Logger.info("Test")
  }
  
}