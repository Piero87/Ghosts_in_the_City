package backend

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.cluster.MemberStatus
import akka.cluster.Member
import common._
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.util.Timeout

/**
 * Entry point of Ghost in the City server backend-side application.
 * It creates an istance of [[Backend]] actor
 */
object Backend {
  
  /**
   * Recover backend parameters from application.conf file.
   * 
   * @param args backend listening port
   */
  def main(args: Array[String]): Unit = {
    // Override the configuration of the port when specified as program argument
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]")).
      withFallback(ConfigFactory.load())

    val system = ActorSystem("GhostsSystem", config)
    system.actorOf(Props[Backend], name = "backend")
  }
}

/**
 * Actor Backend implementation class.
 * Its akka.actor.Props object is defined in application.conf file because loaded in 
 * main method because also Backend actor is a child of system actor.
 */
class Backend extends Actor {

  import Backend._
  
  val cluster = Cluster(context.system)
  var game_manager_backends: List[ActorRef] = List()
  
  val logger = new CustomLogger("Backend")
  
  /**
   * The method preStart() of an actor is only called once directly during the initialization of the first instance,
   * that is, at creation of its ActorRef. In that method we subscribe the backend to the cluster. 
   */
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp]) 
  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  
  /**
   * When the Backend actor stop we unsubscribe it from the cluster
   */
  override def postStop(): Unit = cluster.unsubscribe(self)

  /**
   * Receive method.
   * It helds all the messages that could be sent to the FrontendManager actor from ClientConnection or server
   */
  def receive = {
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register
    case MemberUp(m) => register(m)
    case NewGame(name,n_players,player,game_area_edge,game_type,ref) =>
      logger.log("NewGame request")
      newGame(name,n_players,player,game_area_edge,game_type,ref)
    case GamesListFiltered(game_type) =>
      gamesList(sender, game_type)
    case Terminated(a) =>
      game_manager_backends = game_manager_backends.filterNot(_ == a)
      
  }

  /**
   * New Game method.
   * It creates a GameManagerBackend actor that will "talk" directly with its respectly GameManagerClient actor. 
   * After that, we will forward to it the new game request.
   */
  def newGame (name: String, n_players: Int, player: PlayerInfo, game_area_edge: Double, game_type: String, ref: ActorRef) = {
    val gm_backend = context.actorOf(Props[GameManagerBackend], name = name)
    context watch gm_backend
    game_manager_backends = game_manager_backends :+ gm_backend
    logger.log("Backend: Actor created, forward message...")
    logger.log("Backend PreForward: "+ref.toString())
    gm_backend forward NewGame(name,n_players,player,game_area_edge,game_type,ref)
    
  }
  
  /**
   * Game List method.
   * It asks to the GameManagerBackend actors saved in game_manager_backends list all the game with waiting status.
   * All the Game object are received like Future objet.
   */
  def gamesList (origin: ActorRef, game_type: String) = {
    logger.log("GameList request")
    implicit val ec = context.dispatcher
    val taskFutures: List[Future[Game]] = game_manager_backends map { gm_be =>
        implicit val timeout = Timeout(5 seconds)
        (gm_be ? GameStatus).mapTo[Game]
    }
    
    //The call to Future.sequence is necessary to transform the List of Future[(String, Int)] into a Future of List[(String, Int)].
    val searchFuture = Future sequence taskFutures
    
    searchFuture.onSuccess {
      case results: List[Game] => origin ! results.filter(x => x.status == StatusGame.WAITING && x.g_type == game_type)
    }
    
  }
  
  /**
   * Register method.
   * It permits to register the backend to the frontend
   */
  def register(member: Member): Unit =
    if (member.hasRole("frontend"))
      context.actorSelection(RootActorPath(member.address) / "user" / "frontend") ! "BackendRegistration"
}
