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
   * UpdatePlayerPosition Events to/from the client side
   */
  case class UpdatePlayerPosition(position: Point[LatLng])
  
  
  /**
   * Actor Props 
   */
  def props(area: Polygon[LatLng], starting_position: Point[LatLng]): Props = Props(new Player(area, starting_position))
  
}

class Player(area: Polygon[LatLng], starting_position: Point[LatLng]) extends Actor{
  
  import Player._
  
  var pos: Point[LatLng] = starting_position
  
  def receive = {
   case UpdatePlayerPosition(position) => 
      Logger.info("Updated position received")
      pos = position
      // Qui farò qualcosa con la nuova posizione. 
      sender ! "Done"
  }
  
}