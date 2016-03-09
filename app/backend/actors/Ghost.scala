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
  
  def props(uid: String, arena: Polygon, position: Point, level: Int, treasure: ActorRef, position_treasure: Point, t_uid: String, game_type: String) = Props(new Ghost(uid, arena,position, level, treasure,position_treasure, t_uid, game_type))
}

class Ghost(uid: String, arena : Polygon, position: Point, level: Int, treasure: ActorRef, position_treasure: Point, t_uid: String, game_type: String) extends Actor {
  
  import context._
  import Ghost._
  
  implicit val timeout = Timeout(5 seconds)
  
  var ghostpos: Point = position
  var last_attack: Long = 0
  var mood = GhostMood.CALM
  var GMbackend: ActorRef = _
  val GameParameters = new GameParameters(game_type)
  val ghost_radius = level * GameParameters.ghost_radius
  val treasure_radius = GameParameters.treasure_radius
  var past_move : Int = -1
  val ghost_step : Double = GameParameters.ghost_step + 2 * level
  
  val logger = new CustomLogger("Ghost " + uid)
  logger.log("Arena: " + arena)
  var update_pos_scheduler : Cancellable = null
  
  def receive = {
    case GhostStart => 
      GMbackend = sender
      update_pos_scheduler = system.scheduler.schedule(0 millis, 500 millis, self, UpdateGhostPosition)
    case UpdateGhostPosition => 
      if(ghostpos.distanceFrom(position_treasure, game_type) < treasure_radius || level == 3){
       mood = GhostMood.CALM
        // Ciclo di vita del fantasma: chiedo al GMBackend le posizioni dei player, calcolo la distanza da ciascuno di essi 
        // se rientra nel range di azione attacco altrimenti mi muovo random
        val future = GMbackend ? PlayersInfo
        future.onSuccess { 
          case Players(players) => 
            
            var found_someone = false
            var target_tastness : Double = 0.0
            var target_info : PlayerInfo = null
            var target_actor : ActorRef = null
            
            players.foreach { p =>
              val (info, actor) = p
              var distance = ghostpos.distanceFrom(info.pos, game_type)
              if (distance < ghost_radius){
                // coefficiente di gustosità del giocatore
                // il "+ 1" è per evitare divisioni per zero nel caso il fantasma sia sopra al giocatore
                var player_tastness = info.gold / (distance + 1)
                if (player_tastness > target_tastness){
                  target_tastness = player_tastness
                  target_info = info
                  target_actor = actor
                  mood = GhostMood.ANGRY
                  found_someone = true
                }
              }
            }
            
            if (found_someone && smellPlayerGold(target_info) > 0){
              attackPlayer(target_info, target_actor)
            } else {
              mood = GhostMood.CALM
              random_move()
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
      if(ghostpos.distanceFrom(position_treasure, game_type) < treasure_radius){
        mood = GhostMood.CALM
      }else{
        mood = GhostMood.TRAPPED
      }
      update_pos_scheduler = system.scheduler.schedule(0 millis, 500 millis, self, UpdateGhostPosition)
  }
  
  
  
  def attackPlayer(player_info: PlayerInfo, player_actor : ActorRef) = {
    
    var gold_available = smellPlayerGold(player_info)
    var player_distance = ghostpos.distanceFrom(player_info.pos, game_type)
    logger.log("Distance to player: " + player_distance)
    
    if (player_distance <= GameParameters.max_action_distance && gold_available > 0) {
      
      var now = System.currentTimeMillis()
      if (now >= last_attack + 1500 ) {
        last_attack = now
        // Giocatore raggiunto! Gli rubo i soldi
        val future = player_actor ? IAttackYou(uid, MsgCodes.PARANORMAL_ATTACK, GameParameters.ghost_hunger(level), 0)
        val result = Await.result(future, timeout.duration).asInstanceOf[Int]
        if(result > 0){
          treasure ! IncreaseGold(result)
        }
      }
      
    } else {
      // Giocatore non abbastanza vicino, mi muovo verso di lui
      /*
      var ghost_move : Int = chooseNextMovement(player_info.pos)
      var new_position : Point = computeNextPosition(ghost_move)
      */
      
      var new_position : Point = ghostpos.stepTowards(player_info.pos, GameParameters.ghost_step, game_type)
      
      logger.log("Current ghost position: " + ghostpos)
      
      if ( acceptablePosition(new_position) ) {
        ghostpos = position
        GMbackend ! GhostPositionUpdate(uid, ghostpos , mood)
      }
      
    }
  }
  
  def returnToTreasure = {
    /*
    var ghost_move : Int = chooseNextMovement(position_treasure)
    var new_position : Point = computeNextPosition(ghost_move)
    */
    
    var new_position : Point = ghostpos.stepTowards(position_treasure, GameParameters.ghost_step, game_type)
    
    GMbackend ! GhostPositionUpdate(uid, ghostpos, mood)
  }
  
  def printList(args: TraversableOnce[_]): Unit = {
    args.foreach(println)
  }
  
  def random_move() : Unit = {
    
    val MAX_ATTEMPTS = 100
    var attempts = 0
    var new_position: Point = null
    var good_position = false
    do {
      new_position = ghostpos.randomStep(GameParameters.ghost_step, game_type)
      attempts += 1
    } while (!acceptablePosition(new_position) && attempts != MAX_ATTEMPTS)
    
    /*
    val rnd = new Random()
    var rnd_move = rnd.nextInt(4)
    var new_position : Point = computeNextPosition(rnd_move)

    if (arena.contains(new_position)) {
      if (level !=3 && new_position.distanceFrom(position_treasure, game_type) >= treasure_radius) {
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
    */
    
    
    
  }
  /*
  def chooseNextMovement(target: Point) : Int = {
    var distance_latitude = target.latitude - ghostpos.latitude
    var distance_longitude = target.longitude - ghostpos.longitude
    var next_move = Movement.STILL
    if (Math.abs(distance_latitude) > Math.abs(distance_longitude) && Math.abs(distance_latitude) > icon_size/4) {
			if (distance_latitude > 0){
				next_move = Movement.RIGHT
			} else {
				next_move = Movement.LEFT
			}
		} else if (Math.abs(distance_latitude) < Math.abs(distance_longitude) && Math.abs(distance_longitude) > icon_size/4) {
	     if (distance_longitude > 0){
         next_move = Movement.DOWN
       } else {
         next_move = Movement.UP
       }
		}
    next_move
  }
  
  
  def computeNextPosition(next_move: Int) : Point = {
    next_move match {
       case Movement.UP => {
         new Point(ghostpos.latitude, ghostpos.longitude - GameParameters.ghost_step)
       }
       case Movement.RIGHT => {
         new Point(ghostpos.latitude + GameParameters.ghost_step, ghostpos.longitude)
       }
       case Movement.DOWN => {
         new Point(ghostpos.latitude, ghostpos.longitude + GameParameters.ghost_step)
       }
       case Movement.LEFT => {
         new Point(ghostpos.latitude - GameParameters.ghost_step, ghostpos.longitude)
       }
       case Movement.STILL => {
         new Point(ghostpos.latitude, ghostpos.longitude)
       }
    }
  }
  */
  
  def acceptablePosition(position: Point) : Boolean = {
    if (arena.contains(position)) {
      logger.log("position contained")
      if (level == 3 || position.distanceFrom(position_treasure, game_type) >= GameParameters.treasure_radius) {
        logger.log("movement permitted")
        return true
      }
    }
    return false
  }
  
  def smellPlayerGold(player : PlayerInfo): Int = {
    var gold_toSteal_double = 0.0
    gold_toSteal_double = player.gold * GameParameters.ghost_hunger(level)
    
    var gold_toSteal = gold_toSteal_double.toInt
    gold_toSteal
  }
  
}