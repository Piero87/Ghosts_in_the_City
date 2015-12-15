package backend.actors

import akka.actor.{ ActorRef, Actor }
import akka.actor.Props
import play.api.Logger
import play.api.libs.json._
import play.extras.geojson.Point
import play.extras.geojson.Polygon
import play.extras.geojson.LatLng


object Player {
  
  /**
   * UpdatePlayerPosition Events from the client side
   */
  case class UpdatePlayerPosition(position: Point[LatLng])

  /**
   * Actor Props 
   */
  def props(area: Polygon[LatLng], starting_position: Point[LatLng], id: Long): Props = Props(new Player(area, starting_position, id))
  
}

class Player(area: Polygon[LatLng], starting_position: Point[LatLng], id: Long) extends Actor{
  
  import Player._
  
  var pos: Point[LatLng] = starting_position
  
  def receive = {
   case UpdatePlayerPosition(position) => 
      Logger.info("Updated position received")
      pos = position
      // Qui farò qualcosa con la nuova posizione o me ne sbatto dopo averla salvata? 
      sender ! "Done"
  }
  
}