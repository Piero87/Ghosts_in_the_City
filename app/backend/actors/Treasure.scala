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
   * Actor Props 
   */
  def props(uid: String, position: Point, loot: Tuple2[Key,Int], needKey: Tuple2[Boolean, Key]) = Props(new Treasure(uid,position,loot,needKey))
}

class Treasure(uid: String, position: Point, loot: Tuple2[Key,Int], needKey: Tuple2[Boolean, Key]) extends Actor{
  
  val logger = new CustomLogger("Treasure")
  var treasure_loot = loot
  var treasure_need_key = needKey
  var status = 0
  
  def receive = {  
 
    case Open(pos_p,keys) =>
      
      var origin = sender
      logger.log("Try to open")
      if (treasure_need_key._1 && (keys.size != 0)) {
        //Controlliamo se tra le chiavi passate c'è quella giusta
        var check = false
        breakable {
          for (key <- keys)
          {
            if (key.getKeyUID == needKey._2.getKeyUID)
            {
              logger.log("Opened")
              origin ! Tuple3(MsgCodes.T_SUCCESS_OPENED,treasure_loot._1,treasure_loot._2)
              var k = treasure_loot._1
              k.setExistKey(false)
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
          origin ! Tuple3(MsgCodes.T_WRONG_KEY,treasure_loot._1,treasure_loot._2)
        }
        
      } else if (treasure_need_key._1 && (keys.size == 0))  {
        //Serve una chiave ma non è stata passata nessuna chiave
        logger.log("Not Open: Need Key")
        origin ! Tuple3(MsgCodes.T_NEEDS_KEY,treasure_loot._1,treasure_loot._2)
      } else if (!treasure_need_key._1) {
        //Non serve nessuna chiave tesoro aperto
        logger.log("Opened: without key")
        status = 1
        origin ! Tuple3(MsgCodes.T_SUCCESS_OPENED,treasure_loot._1,treasure_loot._2)
      }
      
    case IncreaseGold(gold) =>
      logger.log("Increase gold")
      treasure_loot = treasure_loot.copy(_2 = treasure_loot._2+gold)
      status = 0
    
  }
  
  
}