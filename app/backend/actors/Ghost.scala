package backend.actors

import akka.actor._
import scala.math._
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import akka.util.Timeout
import akka.pattern.ask
import common._
import scala.util.Random
import scala.collection.mutable.MutableList
import com.typesafe.config.ConfigFactory

object Ghost{
  
  /**
   * Event sent from the Ghost when move itself
   */
  case object UpdateGhostPosition
  
  def props(uid: String, area: Polygon, position: Point, level: Int, treasure: ActorRef,position_treasure: Point, t_uid: String) = Props(new Ghost(uid, area,position, level, treasure,position_treasure, t_uid))
}

class Ghost(uid: String, area : Polygon, position: Point, level: Int, treasure: ActorRef, position_treasure: Point, t_uid: String) extends Actor {
  
  import context._
  import Ghost._
  
  implicit val timeout = Timeout(5 seconds)
  
  var ghostpos: Point = position
  var mood = GhostMood.CALM
  var GMbackend: ActorRef = _
  val ghost_radius = level * ConfigFactory.load().getDouble("ghost_radius")
  val treasure_radius = ConfigFactory.load().getDouble("treasure_radius")
  var past_move : Int = -1
  val ghostmovement: Int = 5 + 2 * level
  val width = ConfigFactory.load().getDouble("space_width")
  val height = ConfigFactory.load().getDouble("space_height")
  val icon_size = ConfigFactory.load().getDouble("icon_size")
  
  val logger = new CustomLogger("Ghost "+uid)
  var update_pos_scheduler : Cancellable = null
  
  def receive = {
    case GhostStart => 
      GMbackend = sender
      update_pos_scheduler = system.scheduler.schedule(0 millis, 500 millis, self, UpdateGhostPosition)
    case UpdateGhostPosition => 
      if(ghostpos.distanceFrom(position_treasure) < treasure_radius){
       mood = GhostMood.CALM
        // Ciclo di vita del fantasma: chiedo al GMBackend le posizioni dei player, calcolo la distanza da ciascuno di essi 
        // se rientra nel range di azione attacco altrimenti mi muovo random
        val future = GMbackend ? PlayersInfo
        future.onSuccess { 
          case Players(players) => 
            var playerpos = new Point(0,0)
            var playerdist : Double = ghost_radius //Max range iniziale
            var found_someone = false
            logger.log("tmp_p")
            val tmp_p = players.map(x => x._1)
            var p_uid = ""
            
            if(tmp_p.size != 0){
              for(player <- tmp_p){
                var currentplayerpos = player.pos
                var distance = ghostpos.distanceFrom(currentplayerpos)
                if(distance < ghost_radius){
                  // Salvo solamente la posizone la cui distanza è minore
                  if(distance < playerdist){
                    playerdist = distance
                    playerpos = currentplayerpos
                    p_uid = player.uid
                  }
                  // Sono incazzato!
                  mood = GhostMood.ANGRY
                  found_someone = true
                }
              }
              
              if (found_someone == false) mood = GhostMood.CALM
              if(mood == GhostMood.CALM){
                random_move(ghostpos)
              }else{
                attackPlayer(p_uid, playerpos, players)
              }
            }else{
              random_move(ghostpos)
            }
        }
        future onFailure {
          case e: Exception => logger.log("******GHOST REQUEST PLAYER POSITION ERROR ******")
        }
      }else{
        returnToTreasure
      }
    case GhostPause =>
      logger.log("In pause")
      update_pos_scheduler.cancel()
    case GhostTrapped(point) =>
      logger.log("Oh no, I'm trapped [" + uid + "]")
      mood = GhostMood.TRAPPED
      ghostpos = point
      update_pos_scheduler.cancel()
    case GhostReleased =>
      if(ghostpos.distanceFrom(position_treasure) < treasure_radius){
        mood = GhostMood.CALM
      }else{
        mood = GhostMood.TRAPPED
      }
      update_pos_scheduler = system.scheduler.schedule(0 millis, 500 millis, self, UpdateGhostPosition)
  }
  
  def random_move(position: Point) : Unit = {
    
/**     val delta_time = 3
*     val speed = 10.0
*     
*     var direction = Math.random() * 2.0 * Math.PI
*     
*     var vx = (speed * Math.cos(direction)).toInt
*     var vy = (speed * Math.sin(direction)).toInt
*     
*     var lat = (vx * delta_time) + position.x
*     var lng = (vy * delta_time) + position.y
*     
*     var new_position = new Point(lat, lng)
**/    
    val rnd = new Random()
    var rnd_pos = rnd.nextInt(4)
    var new_position : Point = position
    rnd_pos match {
      // In alto
      case 0 => {
        if(past_move != 2){
          past_move = rnd_pos
          new_position = new Point(position.x, position.y - ghostmovement)
        }
      }
      // A destra
      case 1 => {
        if(past_move != 3){
          past_move = rnd_pos
          new_position = new Point(position.x + ghostmovement, position.y)
        }
      }
      // In basso
      case 2 => {
        if(past_move != 0){
          past_move = rnd_pos
          new_position = new Point(position.x, position.y + ghostmovement)
        }
      }
      // A sinistra
      case 3 => {
        if(past_move != 1){
          past_move = rnd_pos
          new_position = new Point(position.x - ghostmovement, position.y)
        }
      }
    }
    
//    if (area.contains(new_position)) { Ray-casting
      if ((icon_size < new_position.x && new_position.x < width-icon_size) && (icon_size < new_position.y && new_position.y < height-icon_size)) {
        if (level !=3 && new_position.distanceFrom(position_treasure) >= treasure_radius) {
          if (rnd_pos<2) {
            rnd_pos = 2-rnd_pos
          } else {
            rnd_pos = Math.abs(2-rnd_pos)
          }
        } else {
          ghostpos = new_position
          GMbackend ! GhostPositionUpdate(uid, ghostpos , mood)
        }
      } else {
        if (rnd_pos<2) {
          rnd_pos = 2-rnd_pos
        } else {
          rnd_pos = Math.abs(2-rnd_pos)
        }
      } 
  }
  
  def attackPlayer(p_uid: String, player_pos: Point, players: MutableList[Tuple2[UserInfo, ActorRef]]) = {
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
      if (Math.abs(distance_x) < 10 && Math.abs(distance_y) < 10) {
        logger.log(" Giocatore raggiunto! Lo attacco")
        // Giocatore raggiunto! Gli rubo i soldi
        var pl = players.filter(_._1.uid == p_uid).head
        val future = pl._2 ? IAttackYou(level)
          future.onSuccess { 
            case GoldStolen(gold) =>
              logger.log("Soldi rubati")
              if(gold > 0){
                GMbackend ! IncreaseGoldRequest(t_uid, gold)
              }
          }
          future onFailure {
            case e: Exception => logger.log("******GHOST ATTACK PLAYER ERROR ******")
        }
      }
		}
    
    ghost_move match {
       // In alto
       case 0 => {
           new_position = new Point(ghostpos.x, ghostpos.y - ghostmovement)
       }
       // A destra
       case 1 => {
           new_position = new Point(ghostpos.x + ghostmovement, ghostpos.y)
       }
       // In basso
       case 2 => {
           new_position = new Point(ghostpos.x, ghostpos.y + ghostmovement)
       }
       // A sinistra
       case 3 => {
           new_position = new Point(ghostpos.x - ghostmovement, ghostpos.y)
       }
       case -1 => {
          
       }
     }
    
//  if(area.contains(new_position, area_Edge)){
    if ((icon_size < new_position.x && new_position.x < width-icon_size) && (icon_size < new_position.y && new_position.y < height-icon_size)) {  
       if(level ==3 || new_position.distanceFrom(position_treasure) < treasure_radius){
         ghostpos = new_position
         GMbackend ! GhostPositionUpdate(uid, ghostpos, mood)
       }
    }
  }
  
  def returnToTreasure = {
    var ghost_move : Int = -1
    var new_position : Point = ghostpos
    var distance_x = position_treasure.x - ghostpos.x
    var distance_y = position_treasure.y - ghostpos.y
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
    ghost_move match {
     // In alto
     case 0 => {
         new_position = new Point(ghostpos.x, ghostpos.y - ghostmovement)
     }
     // A destra
     case 1 => {
         new_position = new Point(ghostpos.x + ghostmovement, ghostpos.y)
     }
     // In basso
     case 2 => {
         new_position = new Point(ghostpos.x, ghostpos.y + ghostmovement)
     }
     // A sinistra
     case 3 => {
         new_position = new Point(ghostpos.x - ghostmovement, ghostpos.y)
     }
     case -1 => {
         
     }
   }
   
     ghostpos = new_position
     GMbackend ! GhostPositionUpdate(uid, ghostpos, mood)
       
  }
  
  def printList(args: TraversableOnce[_]): Unit = {
    args.foreach(println)
  }
  
  
     
  
}