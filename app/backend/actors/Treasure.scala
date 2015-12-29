package backend.actors

import akka.actor.{ ActorRef, Actor }
import akka.actor.Props
import play.api.Logger
import common._

import backend.actors.models._

object Treasure {
  
  /**
   * Open Events from the client for treasures locked with a key
   */
  case class Open(Key: Key)
  
  /**
   * TakeGold Events from the client side
   */
  case object TakeGold
  
  /**
   * ReceiveGold Events from the Ghost who guards the treasure
   */
  case class ReceiveGold(ghostGold: Gold)
  
  /**
   * Actor Props 
   */
  def props(position: Point, key: Key, gold: Gold, keyIn: Key) = Props(new Treasure(position, key, gold, keyIn))
}

class Treasure(position: Point, key: Key, gold: Gold, keyIn: Key) extends Actor{
  
  import Treasure._
  
  val treasure_pos = position
  var treasure_gold = gold
  var treasure_key = key
  var treasure_keyIn = keyIn
  
  def receive = {  
 
    case Open(key) => 
      Logger.info("Try to open the treasure")
      if (treasure_key.requested) {
        if (treasure_key.key_id == key.key_id) {
          // C'è un tesoro o una chiave? Controllo solo l'oro perché al momento della creazione del tesoro se metto true  
          // nel gold implicitamente abbiamo false nel key
          if (treasure_gold.present) {
            // Gold
            var award = new Gold(treasure_gold.present, treasure_gold.amount)
            treasure_gold.present = false
            treasure_gold.amount = 0
          } else {
            // Key
            var key_recovered = new Key(treasure_keyIn.requested, treasure_keyIn.key_id)
            treasure_keyIn.requested = false
            //treasure_keyIn.key_id = 0
          }
          sender ! "Opened" // Aggiungerò anche quello che ho raccolto
        } else {
          sender ! "Wrong key"
        }
      } else {
        // Nessuna chiave richiesta controllo il contenuto(se è una chiave o dell'oro) e lo prendo
        if (treasure_gold.present) {
            // Gold
            var award = new Gold(treasure_gold.present, treasure_gold.amount)
            treasure_gold.present = false
            treasure_gold.amount = 0
          } else {
            // Key
            var key_recovered = new Key(treasure_keyIn.requested, treasure_keyIn.key_id)
            treasure_keyIn.requested = false
            //treasure_keyIn.key_id = 0
          }
      }
      sender ! "Opened"// Aggiungerò anche quello che ho raccolto
    
    case ReceiveGold(gold) =>
      Logger.info("Ghost restore gold")
      // Ripristino l'oro recuperato dall'utente
      treasure_gold.present = gold.present
      treasure_gold.amount = gold.amount
  }
  
}