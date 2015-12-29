package backend.actors

import akka.actor._
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
  
  def props(area: Polygon, position: Point, level: Int, treasure: ActorRef) = Props(new Ghost(area,position, level, treasure))
}

class Ghost(area : Polygon, position: Point, level: Int, treasure: ActorRef) extends Actor {
  
  import context._
  import Ghost._
  
  implicit val timeout = Timeout(5 seconds)
  
  var ghostpos: Point = position
  var mood = GhostMood.CALM
  var GMbackend: ActorRef = _
  val range = level * 75
  
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
          var playerpos = new Point(0,0)
          var playerdist : Double = 500
          for(player <- players){
            var currentplayerpos = new Point(player.x, player.y)
            var distance = Math.sqrt(Math.pow((currentplayerpos.x - ghostpos.y),2) + Math.pow((currentplayerpos.x - ghostpos.y),2))
            if(distance < range){
              // Salvo solamente la posizone la cui distanza è minore
              if(distance < playerdist){
                playerdist = distance
                playerpos = currentplayerpos 
              }
              // Sono incazzato!
              mood = GhostMood.ANGRY
            }else{
              // Nessuno all'interno del range
              mood = GhostMood.CALM
            }
          }
          if(mood == GhostMood.CALM){
            random_move(ghostpos)
          }else{
            attackPlayer(playerpos)
          }
          system.scheduler.scheduleOnce(500 millis, self, UpdateGhostPosition)
        }
      future onFailure {
        case e: Exception => Logger.info("******GHOST REQUET PLAYER POSITION ERROR ******")
      }
  }
  
  def random_move(position: Point) = {
     val delta_time = 3
     val speed = 10.0
     
     var direction = Math.random() * 2.0 * Math.PI
     
     var vx = (speed * Math.cos(direction)).toInt
     var vy = (speed * Math.sin(direction)).toInt
     
     var lat = (vx * delta_time) + position.x
     var lng = (vy * delta_time) + position.y
     
     var new_position = new Point(lat, lng)
     
     ghostpos = new_position
     
     context.parent ! ghostpos
     
  }
  
  def attackPlayer(player_pos: Point) = {
    
  }
  
  //schedulo tramite il tick per richiamare il metodo
  def scheduler() = {
     system.scheduler.scheduleOnce(500 millis, self, UpdateGhostPosition)
     
  }
  
  
  
}