package backend.actors

import akka.actor.{ ActorRef, Actor }
import akka.actor.Props
import play.api.Logger
import play.api.libs.json._
import common._
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Future
import backend.actors.models._
import akka.actor._
import akka.util.Timeout
import scala.util.Random

object Player {

  /**
   * Factory for [[backend,clientactor.Player]] instances.
   */
  def props(uid: String, name: String, team: Int, GMbackend: ActorRef): Props = Props(new Player(uid, name, team, GMbackend))
  
}

/**
 * Actor Player implementation class.
 * It manages all the actions that a user client send to the application. 
 * It also manage the race condition using the actor message queue
 * that we could have during an attack from two or more ghosts
 * 
 * @constructor create a new actor with name, uid, GameManager backend actor.
 * @param uid 
 * @param name
 * @param team Websocket 
 * @param GMbackend GameManagerCackend 
 */
class Player(uid: String, name: String, team: Int, GMbackend: ActorRef) extends Actor{
  
  val logger = new CustomLogger("PlayerActor")
  
  /**
   * Receive method.
   * It helds all the messages that could be sent to the Player actor from Ghost and GameManager Backend
   */
  def receive = {
    case SetTrap(gold, pos) =>
      var origin = sender
      if (gold >= 100) {
        // You could set a trap! You have enough money
        var new_gold = gold - 100
        origin ! NewTrap(uid,new_gold, pos)
      } else {
        origin ! MessageCode(uid, MsgCodes.NO_TRAP,"")
      }
    case OpenTreasure(treasures,player) =>
      
      var origin = sender
      implicit val ec = context.dispatcher
      val taskFutures: List[Future[Tuple5[Int,Key,Int,String,String]]] = treasures map { t =>
          implicit val timeout = Timeout(5 seconds)
          (t ? Open(player.pos,player.keys)).mapTo[Tuple5[Int,Key,Int,String,String]]
      }
      
      //The call to Future.sequence is necessary to transform the List of Future[(String, Int)] into a Future of List[(String, Int)].
      val searchFuture = Future sequence taskFutures
      
      searchFuture.onSuccess {
        case results: List[Tuple5[Int,Key,Int,String,String]] =>
          //Fare qualcosa
          logger.log("risultato apertura tesori")
          origin ! TreasureResponse(player.uid, results)
      }
    case IAttackYou(attacker_uid, attack_type, gold_perc_stolen, key_stolen) =>
      if(attack_type == MsgCodes.HUMAN_ATTACK){
        logger.log("Ti ho attaccato")
      }
      GMbackend forward PlayerAttacked(uid, attacker_uid, attack_type, gold_perc_stolen, key_stolen)
      
    // I've attacked another player  
    case AttackHim(victim) =>
      logger.log("Attack him")
      val rnd = new Random()
      var rnd_gold_perc_stolen = rnd.nextDouble
      var keys_stolen = math.round(rnd.nextFloat)
      victim ! IAttackYou(uid, MsgCodes.HUMAN_ATTACK, rnd_gold_perc_stolen, keys_stolen)
       
  }
}