package backend.actors

import akka.actor._
import play.extras.geojson._
import scala.math._
import play.api.Logger
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import akka.util.Timeout
import akka.pattern.ask
import common._

object Ghost{
  
  /**
   * Event sent from the GM when they start moved
   */
  case object Start
  
  /**
   * Event sent from the Ghost when move itself
   */
  case object UpdateGhostPosition
  
  def props(area: Polygon[LatLng], position: Point[LatLng], level: Int, treasure: ActorRef) = Props(new Ghost(area,position, level, treasure))
}

class Ghost(area :Polygon[LatLng], position: Point[LatLng], level: Int, treasure: ActorRef) extends Actor {
  
  import context._
  import Ghost._
  
  implicit val timeout = Timeout(5 seconds)
  
  var ghostpos: Point[LatLng] = position
  var mood = GhostStatus.CALM
  var GMbackend: ActorRef = _
  val range = mood * 75
  
  def receive = {
    case Start => 
      GMbackend = sender
      scheduler()
    case UpdateGhostPosition => 
      Logger.info("Ghost: Updated position received")
      // Ciclo di vita del fantasma: chiedo al GMBackend le posizioni dei player, calcolo la distanza da ciascuno di essi 
      // se rientra nel range di azione attacco altrimenti mi muovo random
      val future = GMbackend ? GiveMePlayerPosition
      future.onSuccess { 
        case Players(players) => 
          Logger.info ("Player positions received")
          var playerpos : Point[LatLng] = null
          var playerdist : Double = 500
          for(player <- players){
            var currentplayerpos = Point(LatLng(player.x,player.y))
            var distance = Math.sqrt(Math.pow((currentplayerpos.coordinates.lat - ghostpos.coordinates.lat),2) + Math.pow((currentplayerpos.coordinates.lng - ghostpos.coordinates.lng),2))
            if(distance < range){
              // Salvo solamente la posizone la cui distanza è minore
              if(distance < playerdist){
                playerdist = distance
                playerpos = currentplayerpos 
              }
              mood = GhostStatus.ANGRY
            }else{
              // Nessuno all'interno del range
              mood = GhostStatus.CALM
            }
          }
          if(mood == GhostStatus.CALM){
            random_move(ghostpos)
          }else{
            attackPlayer(playerpos)
          }
          system.scheduler.scheduleOnce(500 millis, self, UpdateGhostPosition)
          }
      future onFailure {
        case e: Exception => Logger.info("******GHOST PLAYER POSITION ERROR ******")
      }
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
     
     ghostpos = new_position
     
     context.parent ! ghostpos
     
  }
  
  def attackPlayer(player_pos: Point[LatLng]) = {
    
  }
  
  //schedulo tramite il tick per richiamare il metodo
  def scheduler() = {
     system.scheduler.scheduleOnce(500 millis, self, UpdateGhostPosition)
     
  }
  
  
  
}