package backend.actors

import akka.actor._
import play.api.libs.json._
import play.extras.geojson._
import play.api.libs.functional.syntax._
import play.api.mvc.WebSocket.FrameFormatter
import scala.math._
import play.api.Logger


object Ghost{
  
  /**
   * Event sent from the GM when they start moved
   */
  case object Start
  
  /**
   * Event sent from the Ghost when move itself
   */
  case class UpdateGhostPosition(position: Point[LatLng])
  
  def props(area: Polygon[LatLng], position: Point[LatLng]) = Props(new Ghost(area,position))
}

class Ghost(area :Polygon[LatLng], position: Point[LatLng]) extends Actor {
  
  import Ghost._
  
  var pos: Point[LatLng] = position
  
  def receive = {
    case Start => start_move(pos)
    case UpdateGhostPosition(position) => 
      Logger.info("Ghost: Updated position received")
      pos = position
      // Qui farò qualcosa con la nuova posizione. 
      sender ! "Done"
  }
  
  def start_move(position: Point[LatLng]) = {
     val delta_time = 3
     val speed = 10.0
     
     var direction = Math.random() * 2.0 * Math.PI
     
     var vx = (speed * Math.cos(direction)).toInt
     var vy = (speed * Math.sin(direction)).toInt
     
     var lat = (vx * delta_time) + position.coordinates.lat
     var lng = (vy * delta_time) + position.coordinates.lng
     
     var new_position = Point(LatLng(lat,lng))
     
     pos = new_position
     
     // invia la nuova posizione
  }
  
  
  
}