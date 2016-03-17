package backend.actors

import akka.actor.{ ActorRef, Actor }
import akka.actor.Props
import play.api.Logger
import common._
import scala.util.control.Breaks._
import backend.actors.models._
import com.typesafe.config.ConfigFactory

object Treasure {
  
  /**
   * Factory for [[backend,actors.Treasures]] instances.
   */
  def props(uid: String, position: Point, loot: Tuple2[Key,Int], needKey: Tuple2[Boolean, Key], GMbackend: ActorRef) = Props(new Treasure(uid,position,loot,needKey,GMbackend))
}

/**
 * Actor Treasure implementation class.
 * It manages all the actions to a treasure. 
 * It also manage the race condition using the actor message queue
 * that we could have during an open action from two or more player.
 * It will also receive the gold from a ghost who have stolen it from a player
 * 
 * @constructor create a new actor with name, uid, GameManager backend actor.
 * @param uid 
 * @param name
 * @param team Websocket 
 * @param GMbackend GameManagerCackend 
 */
class Treasure(uid: String, position: Point, loot: Tuple2[Key,Int], needKey: Tuple2[Boolean, Key], GMbackend: ActorRef) extends Actor{
  
  val logger = new CustomLogger("Treasure")
  var treasure_loot = loot
  var treasure_need_key = needKey
  //0 = Chiuso
  //1 = Aperto
  var status = 0
  
  /**
   * Receive method.
   * It helds all the messages
   */
  def receive = {  
 
    case Open(pos_p,keys) =>
      
      var origin = sender
      logger.log("Try to open")
      if (treasure_need_key._1 && (keys.size != 0)) {
        // Check if the key treasure needed is present in the keys list
        var check = false
        breakable {
          for (key <- keys)
          {
            if (key.getKeyUID == needKey._2.getKeyUID)
            {
              logger.log("Opened")
              // Tupla5[msg_code, key, gold, uid_treasure, uid key used]
              // The key will be removed if used
              origin ! Tuple5(MsgCodes.T_SUCCESS_OPENED,treasure_loot._1,treasure_loot._2,uid,key.getKeyUID)
              var k = new Key("")
              treasure_loot = Tuple2(k,0)
              treasure_need_key = treasure_need_key.copy(_1 = false)
              check = true
              status = 1
              break
            }
          }
        }
        
        if (!check)  {
          logger.log("Not Open: Wrong key")
          origin ! Tuple5(MsgCodes.T_NEEDS_KEY,treasure_loot._1,treasure_loot._2,uid,"")
        }
        
      } else if (treasure_need_key._1 && (keys.size == 0))  {
        // Treasure needs key to be opened but we don't have one
        logger.log("Not Open: Need Key")
        origin ! Tuple5(MsgCodes.T_NEEDS_KEY,treasure_loot._1,treasure_loot._2,uid,"")
      } else if (!treasure_need_key._1) {
        // No key needed
        logger.log("Opened: without key")
        origin ! Tuple5(MsgCodes.T_SUCCESS_OPENED,treasure_loot._1,treasure_loot._2,uid,"")
        status = 1
        var k = new Key("")
        treasure_loot = Tuple2(k,0)
      }
      
    case IncreaseGold(gold) =>
      logger.log("Increase gold")
      treasure_loot = treasure_loot.copy(_2 = treasure_loot._2+gold)
      status = 0
      GMbackend ! UpdateTreasure(uid,status)
    
  }
  
  
}