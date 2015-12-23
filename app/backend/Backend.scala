package backend

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.cluster.MemberStatus
import akka.cluster.Member
import play.api.Logger
import common._
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.util.Timeout

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
  var game_manager_backends: List[ActorRef] = List()
  
  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register
    case MemberUp(m) => register(m)
    
    case NewGame(name,n_players,user,ref) =>
      Logger.info("Backend: NewGame request")
      newGame(name,n_players,user,ref)
    case GamesList =>
      gamesList(sender)
    case Terminated(a) =>
      game_manager_backends = game_manager_backends.filterNot(_ == a)
      
  }

  def newGame (name: String, n_players: Int, user: UserInfo, ref: ActorRef) = {
    val gm_backend = context.actorOf(Props[GameManagerBackend], name = name)
    context watch gm_backend
    game_manager_backends = game_manager_backends :+ gm_backend
    Logger.info("Backend: Actor created, forward message...")
    Logger.info("Backend PreForward: "+ref.toString())
    gm_backend forward NewGame(name,n_players,user,ref)
    
  }
  
  def gamesList (origin: ActorRef) = {
    implicit val ec = context.dispatcher
    val taskFutures: List[Future[Game]] = game_manager_backends map { gm_be =>
        implicit val timeout = Timeout(5 seconds)
        (gm_be ? GameStatus).mapTo[Game]
    }
    
    //The call to Future.sequence is necessary to transform the List of Future[(String, Int)] into a Future of List[(String, Int)].
    val searchFuture = Future sequence taskFutures
    
    searchFuture.onSuccess {
      case results: List[Game] => origin ! results.filter( _.status == StatusGame.WAITING )
    }
    
  }
    
  def register(member: Member): Unit =
    if (member.hasRole("frontend"))
      context.actorSelection(RootActorPath(member.address) / "user" / "frontend") !
        "BackendRegistration"
}
