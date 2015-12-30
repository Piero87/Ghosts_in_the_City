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
import util.Random.nextInt

object Ghost{
  
  /**
   * Event sent from the GM when they start moved
   */
  case object Start
  
  /**
   * Event sent from the Ghost when move itself
   */
  case object UpdateGhostPosition
  
  def props(uuid: String, area: Polygon, position: Point, level: Int, treasure: ActorRef) = Props(new Ghost(uuid, area,position, level, treasure))
}

class Ghost(uuid: String, area : Polygon, position: Point, level: Int, treasure: ActorRef) extends Actor {
  
  import context._
  import Ghost._
  
  implicit val timeout = Timeout(5 seconds)
  
  var ghostpos: Point = position
  var mood = GhostMood.CALM
  var GMbackend: ActorRef = _
  val range = level * 75
  val area_Edge = area.foundEdge
  
  def receive = {
    case Start => 
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
          if(players.size == 0){
            for(player <- players){
              var currentplayerpos = player.pos
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
          }else{
            random_move(ghostpos)
          }
        }
      future onFailure {
        case e: Exception => Logger.info("******GHOST REQUET PLAYER POSITION ERROR ******")
      }
  }
  
  def random_move(position: Point) = {
    
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
    var rnd_pos = nextInt(4)
    var new_position : Point = null
    rnd_pos match {
      // In alto
      case 0 => new_position = new Point(position.x + 5, position.y)
      // A destra
      case 1 => new_position = new Point(position.x, position.y+ 5)
      // In basso
      case 2 => new_position = new Point(position.x, position.y - 5)
      // A sinistra
      case 3 => new_position = new Point(position.x - 5, position.y)
    }
    
    //if(area.contains(new_position, area_Edge)){
      ghostpos = new_position
    //}else{
      //ghostpos = position
    //}
    
    GMbackend ! GhostPositionUpdate(uuid, ghostpos)
     
  }
  
  def attackPlayer(player_pos: Point) = {
    
    var ghost_move : Int = -1
    var new_position : Point = null
    var distance_x = player_pos.x - ghostpos.x
    var distance_y = player_pos.y - ghostpos.y
    if(Math.abs(distance_x) < range && Math.abs(distance_y) < range){
      if (Math.abs(distance_x) > Math.abs(distance_y) && Math.abs(distance_x) > 10) {
					if (distance_x > 0){
						ghost_move = 1;
					} else {
						ghost_move = 3;
					}
				} else if (Math.abs(distance_x) < Math.abs(distance_y) && Math.abs(distance_y) > 10) {
					if (distance_y > 0){
						ghost_move = 2;
					} else {
						ghost_move = 0;
					}
				}
			}
   ghost_move match {
      // In alto
      case 0 => new_position = new Point(ghostpos.x + 5, ghostpos.y)
      // A destra
      case 1 => new_position = new Point(ghostpos.x, ghostpos.y+ 5)
      // In basso
      case 2 => new_position = new Point(ghostpos.x, ghostpos.y - 5)
      // A sinistra
      case 3 => new_position = new Point(ghostpos.x - 5, ghostpos.y)
   }
   
   //if(area.contains(new_position, area_Edge)){
      ghostpos = new_position
    //}else{
      //ghostpos = position
    //}
    
    GMbackend ! GhostPositionUpdate(uuid, ghostpos)
   
  }
  
  //schedulo tramite il tick per richiamare il metodo
  def scheduler() = {
     system.scheduler.scheduleOnce(500 millis, self, UpdateGhostPosition)
     
  }
  
  
  
}