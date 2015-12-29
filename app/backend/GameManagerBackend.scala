package backend

import akka.actor._
import play.api.Logger
import common._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.util.{Failure, Success}
import util.Random.nextInt
import backend.actors._
import backend.actors.models._

class GameManagerBackend () extends Actor {
  
  var gameManagerClient: ActorRef = _
  var players: List[UserInfo] = List()
  var ghosts: List[Tuple2[GhostInfo, ActorRef]] = List()
  var treasures: List[Tuple2[TreasureInfo, ActorRef]] = List()
  var game_name = ""
  var game_id = java.util.UUID.randomUUID.toString
  var game_n_players = 0
  var game_status = StatusGame.WAITING
  
  implicit val timeout = Timeout(5 seconds)
  implicit val ec = context.dispatcher
  
  def receive = {
    case NewGame(name,n_players,user,ref) =>
      Logger.info("GameManagerBackend: NewGame request")
      game_name = name
      game_n_players = n_players
      game_status = StatusGame.WAITING
      Logger.info("GMBackend NewGame From: "+ref.toString())
      gameManagerClient = ref
      var rnd_team = selectTeam()
      val p = new UserInfo(user.uid,user.name,rnd_team,user.x,user.y)
      players = players :+ p
      val tmp_g = ghosts.map(x => x._1).toList
      val tmp_t = treasures.map(x => x._1).toList
      var g = new Game(game_id,name,n_players,game_status,players,tmp_g,tmp_t)
      sender ! GameHandler(g,self)
    case GameStatus =>
      val tmp_g = ghosts.map(x => x._1).toList
      val tmp_t = treasures.map(x => x._1).toList
      sender ! Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t)
    case JoinGame(game,user,ref) =>
      Logger.info("GMBackend richiesta join ricevuta")
      if (players.size < game_n_players) {
        Logger.info("GMBackend richiesta join accettata")
        //Scegliamo un Team Random Blu o Rosso
        var rnd_team = selectTeam()
        
        val p = new UserInfo(user.uid,user.name,rnd_team,user.x,user.y)
        players = players :+ p
        val tmp_g = ghosts.map(x => x._1).toList
        val tmp_t = treasures.map(x => x._1).toList
        sender ! Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t)
        //Ora mandiamo il messaggio di update game status a tutti i giocatori (***Dobbiamo evitare di mandarlo a quello che si è
        //appena Joinato?
        gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t))
        if (players.size == game_n_players) {
          
          //Qui dovrà generare i fantasmi e i tesori
          for (i <- 0 to game_n_players) {
            var polygon = new Polygon(List(Point(0,0),Point(0,500),Point(500,0),Point(500,500)))
            
            var treasure_id = randomString(8)
            //il boolean qui sotto si può fare random
            var key = new Key(true,randomString(8))
            //qui entrmabi i valori sono random
            var gold = new Gold(true, 100)
            var treasure_info = new TreasureInfo(treasure_id,0,0,0) 
            val treasure = context.actorOf(Props(new Treasure(Point (0,0),key,gold,key)), name = treasure_id)
            treasures = treasures :+ Tuple2(treasure_info,treasure)
            
            var ghost_id = randomString(8)
            val ghost = context.actorOf(Props(new Ghost(polygon,Point (0,0),0,null)), name = ghost_id)
            var ghost_info = new GhostInfo(ghost_id,0,GhostMood.CALM,0,0)
            ghosts = ghosts :+ Tuple2(ghost_info,ghost)
          }
          
          game_status = StatusGame.STARTED
          val tmp_g = ghosts.map(x => x._1).toList
          val tmp_t = treasures.map(x => x._1).toList
          gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t))
          // context.system.scheduler.scheduleOnce(1000 millis, self, "tick")
        }
      } else {
        //***Failure message
        Logger.info("GMB: non ci sono più posti per la partita, attaccati al cazzo")
      }
    case LeaveGame(user: UserInfo) =>
      Logger.info("GMBackend: LeaveGame Request") 
      players = players.filterNot(elm => elm.uid == user.uid)
      sender ! Success
     
      // Se non abbiamo più giocatori dobbiamo dire al GameManager Client  di uccidersi
      if (players.size == 0 || game_status == StatusGame.STARTED) {
        game_status = StatusGame.FINISHED
        val future = gameManagerClient ? KillYourself
          future.onSuccess { 
            case KillMyself => 
              Logger.info ("GameManagerBackend: GMClient will die")
              self ! PoisonPill
          }
          future onFailure {
            case e: Exception => Logger.info("******GAME MANAGER BACKEND KILL ERROR ******")
          }
      } else {
        val tmp_g = ghosts.map(x => x._1).toList
        val tmp_t = treasures.map(x => x._1).toList
        // Ci sono ancora giocatori nella lista quindi aggiorna lo stato
        gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t))
      }
    case UpdatePosition(user) =>
      var i = 0
      for(user <- players) {
        if (user.uid == user.uid) {
          val p = new UserInfo(user.uid,user.name,user.team,user.x,user.y)
          players.updated(i,p)
        }
        i = i + 1
      }
      sender ! BroadcastUpdatePosition(user)
      
    case "tick" =>
      Logger.info("Tick")
//      //qui
//      players = players.map{ player => 
//        val rnd = new scala.util.Random
//        val range = 100 to 500
//        player.x = 0
//        player.y = range(rnd.nextInt(range length))
//        
//      }
//      context.system.scheduler.scheduleOnce(1000 millis, self, "tick")
      
  }
  
  def newGame () = {
      //...inizializza attori partita
  }
  
  def selectTeam() = {
    var rnd_team = nextInt(2)
    //Restituisce il numero di giocatori che appartengono già a quel tipo appena selezionato
    var count_rnd_team = players.count(_.team == rnd_team)
                  
    if (!(count_rnd_team < game_n_players/2)) {
      //Opposto
      rnd_team = 1-rnd_team
    }
    
    rnd_team
  }
  
  def randomString(length: Int) = scala.util.Random.alphanumeric.take(length).mkString
}
