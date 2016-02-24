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
  
  val logger = new CustomLogger("PlayerActor")
  
  def receive = {
    case SetTrap(gold, pos) =>
      var origin = sender
      if (gold >= 100) {
        //Puoi mettere la trappola
        var new_gold = gold - 100
        origin ! NewTrap(uid,new_gold, pos)
      } else {
        origin ! MessageCode(uid, MsgCodes.NO_TRAP)
      }
  }
}