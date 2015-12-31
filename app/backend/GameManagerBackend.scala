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
import util.control.Breaks._
import common.UtilFunctions
import scala.collection.mutable.MutableList

class GameManagerBackend () extends Actor {
  
  var gameManagerClient: ActorRef = _
  var players: MutableList[UserInfo] = MutableList()
  var ghosts: MutableList[Tuple2[GhostInfo, ActorRef]] = MutableList()
  var treasures: MutableList[Tuple2[TreasureInfo, ActorRef]] = MutableList()
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
      Logger.info("GMBackend richiesta join ricevuta")
      if (players.size < game_n_players) {
        Logger.info("GMBackend richiesta join accettata")
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
          
          var width = 500
          var height = 500
          
          var polygon = new Polygon(List(Point(0,0),Point(0,height),Point(width,0),Point(width,height)))
          
          val n_treasure = game_n_players+1
          
          var spaces = UtilFunctions.createSpaces(n_treasure, width, width)
          var position_treasure = new Array[(Double,Double)](n_treasure)
          var position_ghosts = new Array[(Double,Double)](n_treasure)
          
          var position_players = new Array[(Double,Double)](game_n_players)
          position_players = UtilFunctions.randomPositionPlayers(spaces(spaces.length - 1), n_treasure-1)
          
          Logger.info("players list BEFORE: " + players)
          
          for(i <- 0 to game_n_players-1){
            val user = players(i)
            val p = new UserInfo(user.uid,user.name,user.team,Point(position_players(i)._1,position_players(i)._2))
            players.updated(i,p)
          }
          
          Logger.info("players list AFTER: " + players)
          
          for(i <- 0 to n_treasure-1){
            position_treasure(i) = UtilFunctions.randomPositionTreasure(spaces(i))
            System.out.println("position tesoro "+i+" ("+position_treasure(i)._1 +", "+position_treasure(i)._2 +")")
          }
          
          for(j <- 0 to n_treasure-1){ //l'ultimo space è dei giocatori e non ha fantasmi
            position_ghosts(j) = UtilFunctions.randomPositionGhosts(position_treasure(j))
            System.out.println("position ghost "+j+" ("+position_ghosts(j)._1 +", "+position_ghosts(j)._2 +")")
          }
          
          //Qui dovrà generare i fantasmi e i tesori
          for (i <- 0 to game_n_players) {
    
            var treasure_id = randomString(8)
            //il boolean qui sotto si può fare random
            var key = new Key(true,randomString(8))
            //qui entrmabi i valori sono random
            var gold = new Gold(true, 100)
            var p_t = new Point (position_treasure(i)._1,position_treasure(i)._2)
            var treasure_info = new TreasureInfo(treasure_id,0,p_t) 
            val treasure = context.actorOf(Props(new Treasure(treasure_id,p_t,key,gold,key)), name = treasure_id)
            treasures = treasures :+ Tuple2(treasure_info,treasure)
            
            var ghost_id = randomString(8)
            var p_g = new Point (position_ghosts(i)._1,position_ghosts(i)._2)
            val g_level = nextInt(2)+1
            val ghost = context.actorOf(Props(new Ghost(ghost_id,polygon,p_g,g_level,null)), name = ghost_id)
            var ghost_info = new GhostInfo(ghost_id,g_level,GhostMood.CALM,p_g)
            ghosts = ghosts :+ Tuple2(ghost_info,ghost)
          }
          
          game_status = StatusGame.STARTED
          val tmp_g = ghosts.map(x => x._1)
          val tmp_t = treasures.map(x => x._1)
          gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t))
          context.system.scheduler.scheduleOnce(500 millis, self, UpdateGhostsPositions)
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
        val tmp_g = ghosts.map(x => x._1)
        val tmp_t = treasures.map(x => x._1)
        // Ci sono ancora giocatori nella lista quindi aggiorna lo stato
        gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,players,tmp_g,tmp_t))
      }
    case UpdatePosition(user) =>
      var i = 0
      for(user <- players) {
        if (user.uid == user.uid) {
          val p = new UserInfo(user.uid,user.name,user.team,user.pos)
          players.updated(i,p)
        }
        i = i + 1
      }
      sender ! BroadcastUpdatePosition(user)
      
    case UpdateGhostsPositions =>
      Logger.info("UpdateGhostsPositions")
      val tmp_g = ghosts.map(x => x._1)
      gameManagerClient ! BroadcastGhostsPositions(tmp_g)
      context.system.scheduler.scheduleOnce(500 millis, self, UpdateGhostsPositions)
    case GhostPositionUpdate(uid, point) =>
      var i = 0
      for(ghost <- ghosts) {
        if (ghost._1.uid == uid) {
          val g = new GhostInfo(ghost._1.uid,ghost._1.level,ghost._1.mood,point)
          ghosts.updated(i,g)
        }
        i = i + 1
      }
    case PlayersPositions =>
      sender ! Players(players)
      
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
