package backend

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.cluster.MemberStatus
import akka.cluster.Member
import play.api.Logger
import common._

object Backend {
  
  def main(args: Array[String]): Unit = {
    // Override the configuration of the port when specified as program argument
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]")).
      withFallback(ConfigFactory.load())

    val system = ActorSystem("TreasuresSystem", config)
    system.actorOf(Props[Backend], name = "backend")
  }
}

class Backend extends Actor {

  import Backend._
  
  val cluster = Cluster(context.system)
  var game_manager_backends = IndexedSeq.empty[ActorRef]
  
  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register
    case MemberUp(m) => register(m)
    
    case NewGame(name) =>
      Logger.info("Backend: NewGame request")
      newGame(name)
      
  }

  def newGame (name: String) = {
    val gm_backend = context.actorOf(Props[GameManagerBackend], name = name)
    game_manager_backends = game_manager_backends :+ gm_backend
    Logger.info("Backend: Actor created, forward message...")
    gm_backend forward NewGame(name)
    
  }
  
  def register(member: Member): Unit =
    if (member.hasRole("frontend"))
      context.actorSelection(RootActorPath(member.address) / "user" / "frontend") !
        "BackendRegistration"
}