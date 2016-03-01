package backend.actors

import akka.actor._
import scala.math._
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.Await;
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
  
  var ghost_level1_damage = ConfigFactory.load().getDouble("ghost_hunger_level1")
  var ghost_level2_damage = ConfigFactory.load().getDouble("ghost_hunger_level2")
  var ghost_level3_damage = ConfigFactory.load().getDouble("ghost_hunger_level3")
  
  val logger = new CustomLogger("Ghost "+uid)
  var update_pos_scheduler : Cancellable = null
  
  def receive = {
    case GhostStart => 
      GMbackend = sender
      update_pos_scheduler = system.scheduler.schedule(0 millis, 500 millis, self, UpdateGhostPosition)
    case UpdateGhostPosition => 
      if(ghostpos.distanceFrom(position_treasure) < treasure_radius || level == 3){
       mood = GhostMood.CALM
        // Ciclo di vita del fantasma: chiedo al GMBackend le posizioni dei player, calcolo la distanza da ciascuno di essi 
        // se rientra nel range di azione attacco altrimenti mi muovo random
        val future = GMbackend ? PlayersInfo
        future.onSuccess { 
          case Players(players) => 
            
            var player_tastness_max : Double = 0.0
            var found_someone = false
            var target_info : UserInfo = null
            var target_actor : ActorRef = null
            
            players.foreach { p =>
              val (info, actor) = p
              var distance = ghostpos.distanceFrom(info.pos)
                if(distance < ghost_radius){
                  // coefficiente di gustosità del giocatore
                  // il "+ 1" è per evitare divisioni per zero nel caso il fantasma sia sopra al giocatore
                  var player_tastness = info.gold / (distance + 1)
                  if(player_tastness >= player_tastness_max){
                    player_tastness_max = player_tastness
                    mood = GhostMood.ANGRY
                    found_someone = true
                    target_info = info
                    target_actor = actor
                  }
                }
            }
            
            if (found_someone == false){
              mood = GhostMood.CALM
              random_move(ghostpos)
            } else {
              attackPlayer(target_info, target_actor)
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
    var rnd_move = rnd.nextInt(4)
    var new_position : Point = computeNextPosition(rnd_move)

    //if (area.contains(new_position)) {
      if ((icon_size < new_position.x && new_position.x < width-icon_size) && (icon_size < new_position.y && new_position.y < height-icon_size)) {
        if (level !=3 && new_position.distanceFrom(position_treasure) >= treasure_radius) {
          if (rnd_move<2) {
            rnd_move = 2-rnd_move
          } else {
            rnd_move = Math.abs(2-rnd_move)
          }
        } else {
          ghostpos = new_position
          GMbackend ! GhostPositionUpdate(uid, ghostpos , mood)
        }
      } else {
        if (rnd_move<2) {
          rnd_move = 2-rnd_move
        } else {
          rnd_move = Math.abs(2-rnd_move)
        }
      } 
  }
  
  def attackPlayer(player_info: UserInfo, player_actor : ActorRef) = {
    var ghost_move : Int = chooseNextMovement(player_info.pos)
    var new_position : Point = computeNextPosition(ghost_move)
    
    //if (area.contains(new_position)){
    if ((icon_size < new_position.x && new_position.x < width-icon_size) && (icon_size < new_position.y && new_position.y < height-icon_size)) {  
       if(level == 3 || new_position.distanceFrom(position_treasure) < treasure_radius){
         ghostpos = new_position
         GMbackend ! GhostPositionUpdate(uid, ghostpos, mood)
       }
    }
    
    var gold_available = smellPlayerGold(player_info)
    
    var distance_x = player_info.pos.x - ghostpos.x
    var distance_y = player_info.pos.y - ghostpos.y
    
    if (Math.abs(distance_x) < icon_size/2 && Math.abs(distance_y) < icon_size/2 && gold_available > 0) {
        logger.log(" Giocatore raggiunto! Lo attacco")
        // Giocatore raggiunto! Gli rubo i soldi
        
        val future = player_actor ? IAttackYou(level)
        val result = Await.result(future, timeout.duration).asInstanceOf[Int]
        if(result > 0){
          treasure ! IncreaseGold(result)
        }
      }
  }
  
  def returnToTreasure = {
    var ghost_move : Int = chooseNextMovement(position_treasure)
    var new_position : Point = computeNextPosition(ghost_move)
    GMbackend ! GhostPositionUpdate(uid, ghostpos, mood)
  }
  
  def printList(args: TraversableOnce[_]): Unit = {
    args.foreach(println)
  }
  
  def chooseNextMovement(target: Point) : Int = {
    var distance_x = target.x - ghostpos.x
    var distance_y = target.y - ghostpos.y
    if (Math.abs(distance_x) > Math.abs(distance_y) && Math.abs(distance_x) > icon_size/4) {
			if (distance_x > 0){
				Movement.RIGHT
			} else {
				Movement.LEFT
			}
		} else if (Math.abs(distance_x) < Math.abs(distance_y) && Math.abs(distance_y) > icon_size/4) {
	     if (distance_y > 0){
         Movement.DOWN
       } else {
         Movement.UP
       }
		}
    Movement.STILL
  }
  
  def computeNextPosition(next_move: Int) : Point = {
    next_move match {
       case Movement.UP => {
         new Point(ghostpos.x, ghostpos.y - ghostmovement)
       }
       case Movement.RIGHT => {
         new Point(ghostpos.x + ghostmovement, ghostpos.y)
       }
       case Movement.DOWN => {
         new Point(ghostpos.x, ghostpos.y + ghostmovement)
       }
       case Movement.LEFT => {
         new Point(ghostpos.x - ghostmovement, ghostpos.y)
       }
       case Movement.STILL => {
         new Point(ghostpos.x, ghostpos.y)
       }
    }
  }
  
  def smellPlayerGold(player : UserInfo): Int = {
    var gold_toSteal_double = 0.0
    level match {
      case 1 => {
        gold_toSteal_double = player.gold * ghost_level1_damage
      }
      case 2 => {
        gold_toSteal_double = player.gold * ghost_level2_damage
      }
      case 3 => {
        gold_toSteal_double = player.gold * ghost_level3_damage
      }
    }
    var gold_toSteal = gold_toSteal_double.toInt
    gold_toSteal
  }
  
  
     
  
}