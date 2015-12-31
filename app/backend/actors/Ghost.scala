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
import scala.util.Random
import com.typesafe.config.ConfigFactory

object Ghost{
  
  /**
   * Event sent from the Ghost when move itself
   */
  case object UpdateGhostPosition
  
  def props(uuid: String, area: Polygon, position: Point, level: Int, treasure: ActorRef,position_treasure: Point) = Props(new Ghost(uuid, area,position, level, treasure,position_treasure))
}

class Ghost(uuid: String, area : Polygon, position: Point, level: Int, treasure: ActorRef, position_treasure: Point) extends Actor {
  
  import context._
  import Ghost._
  
  implicit val timeout = Timeout(5 seconds)
  
  var ghostpos: Point = position
  var mood = GhostMood.CALM
  var GMbackend: ActorRef = _
  val ghost_radius = level * ConfigFactory.load().getDouble("ghost_radius")
  val treasure_radius = ConfigFactory.load().getDouble("treasure_radius")
  val area_Edge = area.foundEdge
  var past_move : Int = -1
  val width = ConfigFactory.load().getDouble("space_width")
  val height = ConfigFactory.load().getDouble("space_height")
  val icon_size = ConfigFactory.load().getDouble("icon_size")
  
  def receive = {
    case GhostStart => 
      GMbackend = sender
      scheduler()
    case UpdateGhostPosition => 
      Logger.info("Ghost: Updated position received")
      // Ciclo di vita del fantasma: chiedo al GMBackend le posizioni dei player, calcolo la distanza da ciascuno di essi 
      // se rientra nel range di azione attacco altrimenti mi muovo random
      val future = GMbackend ? PlayersPositions
      future.onSuccess { 
        case Players(players) => 
          Logger.info ("Player positions received")
          var playerpos = new Point(0,0)
          var playerdist : Double = 500
//          if(players.size == 0){
//            for(player <- players){
//              var currentplayerpos = player.pos
//              var distance = distanceFrom(currentplayerpos)
//              if(distance < range){
//                // Salvo solamente la posizone la cui distanza è minore
//                if(distance < playerdist){
//                  playerdist = distance
//                  playerpos = currentplayerpos 
//                }
//                // Sono incazzato!
//                mood = GhostMood.ANGRY
//              }else{
//                // Nessuno all'interno del range
//                mood = GhostMood.CALM
//              }
//            }
//            if(mood == GhostMood.CALM){
//              random_move(ghostpos)
//            }else{
//              attackPlayer(playerpos)
//            }
//          }else{
//            random_move(ghostpos)
//          }
          random_move(ghostpos)
        }
      future onFailure {
        case e: Exception => Logger.info("******GHOST REQUET PLAYER POSITION ERROR ******")
      }
  }
  
  def random_move(position: Point) : Unit = {
    
//     val delta_time = 3
//     val speed = 10.0
//     
//     var direction = Math.random() * 2.0 * Math.PI
//     
//     var vx = (speed * Math.cos(direction)).toInt
//     var vy = (speed * Math.sin(direction)).toInt
//     
//     var lat = (vx * delta_time) + position.x
//     var lng = (vy * delta_time) + position.y
//     
//     var new_position = new Point(lat, lng)
//    
    val rnd = new Random()
    var rnd_pos = rnd.nextInt(4)
    var new_position : Point = position
    rnd_pos match {
      // In alto
      case 0 => {
        if(past_move != 2){
          past_move = rnd_pos
          new_position = new Point(position.x + 5, position.y)
        }
      }
      // A destra
      case 1 => {
        if(past_move != 3){
          past_move = rnd_pos
          new_position = new Point(position.x, position.y+ 5)
        }
      }
      // In basso
      case 2 => {
        if(past_move != 0){
          past_move = rnd_pos
          new_position = new Point(position.x, position.y - 5)
        }
      }
      // A sinistra
      case 3 => {
        if(past_move != 1){
          past_move = rnd_pos
          new_position = new Point(position.x - 5, position.y)
        }
      }
    }
    
//    if (area.contains(new_position, area_Edge)) {
      if ((new_position.x < width-icon_size) || 
          (new_position.y < height-icon_size)) {
        if (distanceFrom(position_treasure) < treasure_radius) {
            ghostpos = new_position
            Logger.info("GHOST: SEND NEW POSITION")
            GMbackend ! GhostPositionUpdate(uuid, ghostpos)
            Logger.info("UUID: " + uuid + " - POS: " + ghostpos)
            scheduler()
        } else {
          past_move = -1
          scheduler()
        }
    } else {
      past_move = -1
      scheduler()
    } 
  }
  
  def attackPlayer(player_pos: Point) = {
    
    var ghost_move : Int = -1
    var new_position : Point = ghostpos
    var distance_x = player_pos.x - ghostpos.x
    var distance_y = player_pos.y - ghostpos.y
    if(Math.abs(distance_x) < ghost_radius && Math.abs(distance_y) < ghost_radius){
      if (Math.abs(distance_x) > Math.abs(distance_y) && Math.abs(distance_x) > 10) {
				if (distance_x > 0){
					ghost_move = 1
				} else {
					ghost_move = 3
				}
			} else if (Math.abs(distance_x) < Math.abs(distance_y) && Math.abs(distance_y) > 10) {
		    if (distance_y > 0){
						ghost_move = 2
				} else {
						ghost_move = 0
				}
			}
		}
    
   ghost_move match {
      // In alto
      case 0 => {
        if(past_move != 2){
          past_move = ghost_move
          new_position = new Point(ghostpos.x + 5, ghostpos.y)
        }
      }
      // A destra
      case 1 => {
        if(past_move != 3){
          past_move = ghost_move
          new_position = new Point(ghostpos.x, ghostpos.y+ 5)
        }
      }
      // In basso
      case 2 => {
        if(past_move != 0){
          past_move = ghost_move
          new_position = new Point(ghostpos.x, ghostpos.y - 5)
        }
      }
      // A sinistra
      case 3 => {
        if(past_move != 1){
          past_move = ghost_move
          new_position = new Point(ghostpos.x - 5, ghostpos.y)
        }
      }
   }
   
//   if(area.contains(new_position, area_Edge)){
//     if(distanceFrom(treasure.position) < treasure_radius){
//        ghostpos = new_position
//        GMbackend ! GhostPositionUpdate(uuid, ghostpos)
//        scheduler()
//    }else{
//        mood = GhostMood.CALM
//        random_move(ghostpos)
//   }else{
//      ghostpos = position
//    }
//       
  }
  
  // Calculate distance
  def distanceFrom(pos: Point) : Double = {
    var dist = Math.sqrt(Math.pow((ghostpos.x - pos.x),2) + Math.pow((ghostpos.y - pos.y),2))
    dist
  }
  
  //schedulo tramite il tick per richiamare il metodo
  def scheduler() = {
     system.scheduler.scheduleOnce(500 millis, self, UpdateGhostPosition)
     
  }
  
}