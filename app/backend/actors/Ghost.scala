package backend.actors

import akka.actor._
import play.api.libs.json._
import play.extras.geojson._
import play.api.libs.functional.syntax._
import play.api.mvc.WebSocket.FrameFormatter
import scala.math._


object Ghost{
  
  /**
   * Event sent from the GM when they start moved
   */
  case object Start
  
  def props(area: Polygon[LatLng], position: Point[LatLng]) = Props(new Ghost(area,position))
}

class Ghost(area :Polygon[LatLng], start_position: Point[LatLng]) extends Actor {
  
  import Ghost._
  
  var pos: Point[LatLng] = start_position
  
  def receive = {
    case Start => start_move(start_position)
  }
  
  def start_move(pos: Point[LatLng]) = {
     val delta_time = 3
     val speed = 10.0
     
     var direction = Math.random() * 2.0 * Math.PI
     
     var vx = (speed * Math.cos(direction)).toInt
     var vy = (speed * Math.sin(direction)).toInt
     
     var lat = (vx * delta_time) + pos.coordinates.lat
     var lng = (vy * delta_time) + pos.coordinates.lng
     
     var new_position = Point(LatLng(lat,lng))
     
     pos = new_position
  }
  
}