package backend

import akka.actor._
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
import util.control.Breaks._
import common.UtilFunctions
import scala.collection.mutable.MutableList
import com.typesafe.config.ConfigFactory

class GameManagerBackend () extends Actor {
  
  var gameManagerClient: ActorRef = _
  var players: MutableList[UserInfo] = MutableList()
  var ghosts: MutableList[Tuple2[GhostInfo, ActorRef]] = MutableList()
  var treasures: MutableList[Tuple2[TreasureInfo, ActorRef]] = MutableList()
  var game_name = ""
  var game_id = java.util.UUID.randomUUID.toString
  var game_n_players = 0
  var game_status = StatusGame.WAITING
  var previous_game_status = -1
  
  var paused_players:MutableList[Tuple2[String, Long]] = MutableList()
  
  val logger = new CustomLogger("GameManagerBackend")
  
  implicit val timeout = Timeout(5 seconds)
  implicit val ec = context.dispatcher
  
  def receive = {
    case NewGame(name,n_players,user,ref) =>
      logger.log("NewGame request")
      game_name = name
      game_n_players = n_players
      game_status = StatusGame.WAITING
      logger.log("GMBackend NewGame From: "+ref.toString())
      gameManagerClient = ref
      var rnd_team = selectTeam()
      val p = new UserInfo(user.uid,user.name,rnd_team,user.pos)
      players = players :+ p
      val tmp_g = ghosts.map(x => x._1)
      val tmp_t = treasures.map(x => x._1)
      var g = new Game(game_id,name,n_players,game_status,players,tmp_g,tmp_t)
      sender ! GameHandler(g,self)
    case GameStatus =>
      val tmp_g = ghosts.map(x => x._1)
      val tmp_t = treasures.map(x => x._1)
      sender ! Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t)
    case JoinGame(game,user,ref) =>
      logger.log("Join Request riceived")
      if (players.size < game_n_players) {
        logger.log("Join Request accepted")
        //Scegliamo un Team Random Blu o Rosso
        var rnd_team = selectTeam()
        
        val p = new UserInfo(user.uid,user.name,rnd_team,user.pos)
        players = players :+ p
        val tmp_g = ghosts.map(x => x._1)
        val tmp_t = treasures.map(x => x._1)
        sender ! Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t)
        //Ora mandiamo il messaggio di update game status a tutti i giocatori (***Dobbiamo evitare di mandarlo a quello che si è
        //appena Joinato?
        gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t))
        if (players.size == game_n_players) {
          newGame ()
        }
      } else {
        //***Failure message
        logger.log("GMB: non ci sono più posti per la partita, attaccati al cazzo")
      }
    case ResumeGame(gameid: String, user: UserInfo, ref: ActorRef) =>
      logger.log("ResumeGame Request")
      paused_players = paused_players.filterNot(elm => elm._1 == user.uid)
      // Svegliamo il giocatore che è appena tornato!
      val tmp_g = ghosts.map(x => x._1)
      val tmp_t = treasures.map(x => x._1)
      sender ! Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t)
    case PauseGame(user: UserInfo) =>
      logger.log("PauseGame Request")
      paused_players = paused_players :+ Tuple2(user.uid, System.currentTimeMillis())
      ghosts.map {ghost =>
        ghost._2 ! GhostPause
      }
      scheduler()
      if(game_status != StatusGame.PAUSED) previous_game_status = game_status
      game_status = StatusGame.PAUSED
      val tmp_g = ghosts.map(x => x._1)
      val tmp_t = treasures.map(x => x._1)
      gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t))
    case LeaveGame(user: UserInfo) =>
      logger.log("LeaveGame Request") 
      players = players.filterNot(elm => elm.uid == user.uid)
      sender ! Success
     
      // Se non abbiamo più giocatori dobbiamo dire al GameManager Client  di uccidersi
      if (players.size == 0 || game_status == StatusGame.STARTED) {
        game_status = StatusGame.FINISHED
        val future = gameManagerClient ? KillYourself
          future.onSuccess { 
            case KillMyself => 
              logger.log("GMClient will die")
              self ! PoisonPill
          }
          future onFailure {
            case e: Exception => logger.log("******GAME MANAGER BACKEND KILL ERROR ******")
          }
      } else {
        val tmp_g = ghosts.map(x => x._1)
        val tmp_t = treasures.map(x => x._1)
        // Ci sono ancora giocatori nella lista quindi aggiorna lo stato
        gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t))
      }
    case UpdatePosition(user) =>
      var i = 0
      for(user_tmp <- players) {
        if (user_tmp.uid == user.uid) {
          val p = new UserInfo(user.uid,user.name,user.team,user.pos)
          players(i) = p
        }
        i = i + 1
      }
      sender ! BroadcastUpdatePosition(user)
      
    case UpdateGhostsPositions =>
      //Logger.info("UpdateGhostsPositionsBroadcast")
      val tmp_g = ghosts.map(x => x._1)
      gameManagerClient ! BroadcastGhostsPositions(tmp_g)
      context.system.scheduler.scheduleOnce(500 millis, self, UpdateGhostsPositions)
    case GhostPositionUpdate(uid, point,mood) =>
      for (i <- 0 to ghosts.size-1) {
        if (ghosts(i)._1.uid == uid) {
          var g = new GhostInfo(ghosts(i)._1.uid,ghosts(i)._1.level,mood,point)
          ghosts(i) = ghosts(i).copy(_1 = g)
        }
      }
    case PlayersPositions =>
      sender ! Players(players)
    case CheckPaused =>
      var now = System.currentTimeMillis()
      if (paused_players.size > 0) {
        for (player <- paused_players) {
          if (now - player._2 > 10000) {
            game_status = StatusGame.FINISHED
            val future = gameManagerClient ? KillYourself
            future.onSuccess { 
              case KillMyself => 
                logger.log("GameManagerBackend: GMClient will die")
                self ! PoisonPill
            }
            future onFailure {
              case e: Exception => logger.log("******GAME MANAGER BACKEND KILL ERROR ******")
            }
          }
        }
        val tmp_g = ghosts.map(x => x._1)
        val tmp_t = treasures.map(x => x._1)
        gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t))
        scheduler()
      } else {
        game_status = previous_game_status
        ghosts.map {ghost =>
          ghost._2 ! GhostStart
        }
        val tmp_g = ghosts.map(x => x._1)
        val tmp_t = treasures.map(x => x._1)
        gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t))
      }
  }
  
  def newGame () = {
      //...inizializza attori partita
    var width = ConfigFactory.load().getDouble("space_width")
    var height = ConfigFactory.load().getDouble("space_height")

    var polygon = new Polygon(List(Point(0,0),Point(0,height),Point(width,0),Point(width,height)))
    
    val n_treasure = game_n_players+1
    
    var spaces = UtilFunctions.createSpaces(n_treasure)
    var position_treasure = new Array[Point](n_treasure)
    var position_ghosts = new Array[Point](n_treasure)
    var free_position_ghosts = new Array[Point](n_treasure)
    
    var position_players = new Array[Point](game_n_players)
    position_players = UtilFunctions.randomPositionsInSpace(spaces(spaces.length - 1), n_treasure-1)
    
    for(i <- 0 to game_n_players-1){
      val user = players(i)
      val p = new UserInfo(user.uid,user.name,user.team,Point(position_players(i).x,position_players(i).y))
      // attore player
      players(i) = p
    }
    
    for(i <- 0 to game_n_players){
      position_treasure(i) = UtilFunctions.randomPositionInSpace(spaces(i))
      logger.log("Treasure[" + i + "] position: ("+ position_treasure(i).x +","+ position_treasure(i).y +")")
    }
    
    for(j <- 0 to game_n_players){ //l'ultimo space è dei giocatori e non ha fantasmi
      position_ghosts(j) = UtilFunctions.randomPositionAroundPoint(position_treasure(j))
      logger.log("Ghost[" + j + "] position: ("+ position_ghosts(j).x +","+ position_ghosts(j).y +")")
    }
    
    for(j <- 0 to game_n_players){ //l'ultimo space è dei giocatori e non ha fantasmi
      free_position_ghosts(j) = UtilFunctions.randomPositionInSpace(spaces(j))
      logger.log("Free Ghost[" + j + "] position: ("+ free_position_ghosts(j).x +","+ free_position_ghosts(j).y +")")
    }
    
    //Qui dovrà generare i fantasmi e i tesori
    for (i <- 0 to game_n_players) {

      var treasure_id = randomString(8)
      //il boolean qui sotto si può fare random
      var key = new Key(true,randomString(8))
      //qui entrmabi i valori sono random
      var gold = new Gold(true, 100)
      var p_t = new Point (position_treasure(i).x,position_treasure(i).y)
      var treasure_info = new TreasureInfo(treasure_id,0,p_t) 
      val treasure = context.actorOf(Props(new Treasure(treasure_id,p_t,key,gold,key)), name = treasure_id)
      treasures = treasures :+ Tuple2(treasure_info,treasure)
      
      // Fantasmi a guardia dei tesori
      var ghost_id = randomString(8)
      var p_g = new Point (position_ghosts(i).x,position_ghosts(i).y)
      val g_level = nextInt(2)+1
      val ghost = context.actorOf(Props(new Ghost(ghost_id,polygon,p_g,g_level,treasure,p_t)), name = ghost_id)
      var ghost_info = new GhostInfo(ghost_id,g_level,GhostMood.CALM,p_g)
      ghosts = ghosts :+ Tuple2(ghost_info,ghost)
      
      // Fantasmi liberi di girare per tutta l'area
      var free_ghost_id = randomString(8)
      var free_p_g = new Point (free_position_ghosts(i).x,free_position_ghosts(i).y)
      val n_treasure = nextInt(treasures.size)
      val free_ghost = context.actorOf(Props(new Ghost(free_ghost_id,polygon,free_p_g,3,treasures(n_treasure)._2,treasures(n_treasure)._1.pos)), name = free_ghost_id)
      var free_ghost_info = new GhostInfo(free_ghost_id,3,GhostMood.CALM,free_p_g)
      ghosts = ghosts :+ Tuple2(free_ghost_info,free_ghost)
    }
    
    game_status = StatusGame.STARTED
    val tmp_g = ghosts.map(x => x._1)
    val tmp_t = treasures.map(x => x._1)
    gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t))
    for (i <- 0 to ghosts.size-1) {
      ghosts(i)._2 ! GhostStart
    }
      
    context.system.scheduler.scheduleOnce(500 millis, self, UpdateGhostsPositions)
    
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
  
  def scheduler() = {
     context.system.scheduler.scheduleOnce(1000 millis, self, CheckPaused) 
  }
}
