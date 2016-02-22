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
  def props(uid: String, name: String, team: Int, area : Polygon): Props = Props(new Player(uid, name, team, area))
  
}

class Player(uid: String, name: String, team: Int, area : Polygon) extends Actor{
  
  var gold = 0
  var position = Point(0,0)
  
  def receive = {
    case SetTrap =>
      var origin = sender
      if (gold >= 100) {
        //Puoi mettere la trappola
        gold = gold - 100
        origin ! NewTrap(position)
      }
    
    case UpdatePlayerPos(pos) =>
      position = pos
  }
}