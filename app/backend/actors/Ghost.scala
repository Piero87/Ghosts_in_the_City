package backend.actors

import akka.actor._
import play.api.libs.json._
import play.extras.geojson._
import play.api.libs.functional.syntax._
import play.api.mvc.WebSocket.FrameFormatter
import scala.math._
import play.api.Logger
import scala.concurrent.duration._


object Ghost{
  
  /**
   * Event sent from the GM when they start moved
   */
  case object Start
  
  /**
   * Event sent from the Ghost when move itself
   */
  case object UpdateGhostPosition
  
  def props(area: Polygon[LatLng], position: Point[LatLng]) = Props(new Ghost(area,position))
}

class Ghost(area :Polygon[LatLng], position: Point[LatLng]) extends Actor {
  
  import context._
  import Ghost._
  
  var pos: Point[LatLng] = position
  // riferimento game manager : ActorRef
  // cerca comando per capire di chi sei figlio e farti restituire il riferimento
  
  def receive = {
    case Start => scheduler()
    case UpdateGhostPosition => 
      Logger.info("Ghost: Updated position received")
      random_move(pos)
      system.scheduler.scheduleOnce(500 millis, self, UpdateGhostPosition)
  }
  
  def random_move(position: Point[LatLng]) = {
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
  
  //schedulo tramite il tick per richiamare il metodo
  def scheduler() = {
     system.scheduler.scheduleOnce(500 millis, self, UpdateGhostPosition)
     
  }
  
  
  
}