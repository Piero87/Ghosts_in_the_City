package backend.actors

import akka.actor.{ ActorRef, Actor }
import akka.actor.Props
import play.api.Logger
import common._
import scala.util.control.Breaks._
import backend.actors.models._

object Treasure {
  
  /**
   * Actor Props 
   */
  def props(uid: String, position: Point, loot: Tuple2[Key,Gold], needKey: Tuple2[Boolean, Key]) = Props(new Treasure(uid,position,loot,needKey))
}

class Treasure(uid: String, position: Point, loot: Tuple2[Key,Gold], needKey: Tuple2[Boolean, Key]) extends Actor{
  
  val logger = new CustomLogger("Treasure")
  var treasure_loot = loot
  var treasure_need_key = needKey
  
  def receive = {  
 
    case Open(keys) =>
      var origin = sender
      logger.log("Try to open")
      if (needKey._1 && (keys.size != 0)) {
        //Controlliamo se tra le chiavi passate c'è quella giusta
        var check = false
        breakable {
          for (key <- keys)
          {
            if (key.getKeyUID == needKey._2.getKeyUID)
            {
              logger.log("Opened")
              origin ! LootRetrieved(treasure_loot)
              treasure_loot = Tuple2(null,null)
              treasure_need_key = treasure_need_key.copy(_1 = false)
              check = true
              break
            }
          }
        }
        
        if (!check)  {
          logger.log("Not Open: Wrong key")
          origin ! TreasureError("Non hai la chiave giusta per questo tesoro")
        }
        
      } else if (needKey._1 && (keys.size == 0))  {
        //Serve una chiave ma non è stata passata nessuna chiave
        logger.log("Not Open: Need Key")
        origin ! TreasureError("Ti serve una chiave per aprire il tesoro")
      } else if (!needKey._1) {
        //Non serve nessuna chiave tesoro aperto
        logger.log("Opened: without key")
        origin ! LootRetrieved(treasure_loot)
      }
    case IncreaseGold(gold) =>
      logger.log("Increase gold")
      treasure_loot = treasure_loot.copy(_2 = new Gold(treasure_loot._2.getAmount+gold.getAmount))
    
  }
  
  
}