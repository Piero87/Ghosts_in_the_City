package actors

import akka.actor._
import play.api.Logger
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import akka.pattern.ask
import scala.concurrent.duration._

case object GetFrontendAddress
case object FrontendAddress

class RecoverFrontend extends Actor {
  
  var frontend: ActorRef = _
  
  override def preStart() = {
    createFrontend()
  }
  
  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}
  
  def createFrontend ()
  {
//    val config = ConfigFactory.parseString("akka.cluster.roles = [frontend]").withFallback(ConfigFactory.load())
//    val actorSystem = ActorSystem("TreasuresSystem", ConfigFactory.load())
//    frontend = actorSystem.actorOf(Props[Frontend], name = "frontend")
  }
  
  def receive = {
    
    case GetFrontendAddress => 
      Logger.info("Retrieving Frontend Address...")
//      implicit val ec = context.dispatcher
//      frontend.ask(Ping)(3.second).
//      map { _ => FrontendAddress
//        }.recover { _ => FrontendAddress
//          }.pipeTo(sender)
  }
}