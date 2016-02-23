package backend

import akka.actor._
import common._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.util.{Failure, Success}
import backend.actors._
import backend.actors.models._
import util.control.Breaks._
import common.UtilFunctions
import scala.collection.mutable.MutableList
import com.typesafe.config.ConfigFactory
import scala.util.Random

class GameManagerBackend () extends Actor {
  
  var gameManagerClient: ActorRef = _
  var players: MutableList[Tuple2[UserInfo, ActorRef]] = MutableList()
  var ghosts: MutableList[Tuple2[GhostInfo, ActorRef]] = MutableList()
  var treasures: MutableList[Tuple2[TreasureInfo, ActorRef]] = MutableList()
  var traps: MutableList[Trap] = MutableList()
  var trap_radius: Double = 0
  
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
      val p = new UserInfo(user.uid,user.name,rnd_team,user.pos,new Gold (0),List())
      players = players :+ Tuple2(p,null)
      val tmp_g = ghosts.map(x => x._1)
      val tmp_t = treasures.map(x => x._1)
      val tmp_p = players.map(x => x._1)
      var g = new Game(game_id,name,n_players,game_status,tmp_p,tmp_g,tmp_t)
      sender ! GameHandler(g,self)
    case GameStatus =>
      val tmp_g = ghosts.map(x => x._1)
      val tmp_t = treasures.map(x => x._1)
      val tmp_p = players.map(x => x._1)
      sender ! Game(game_id,game_name,game_n_players,game_status,tmp_p,tmp_g,tmp_t)
    case JoinGame(game,user,ref) =>
      logger.log("Join Request riceived")
      if (players.size < game_n_players) {
        logger.log("Join Request accepted")
        //Scegliamo un Team Random Blu o Rosso
        var rnd_team = selectTeam()
        
        val p = new UserInfo(user.uid,user.name,rnd_team,user.pos,new Gold(0),List())
        players = players :+ Tuple2(p,null)
        val tmp_g = ghosts.map(x => x._1)
        val tmp_t = treasures.map(x => x._1)
        val tmp_p = players.map(x => x._1)
        sender ! Game(game_id,game_name,game_n_players,game_status,tmp_p,tmp_g,tmp_t)
        //Ora mandiamo il messaggio di update game status a tutti i giocatori (***Dobbiamo evitare di mandarlo a quello che si è
        //appena Joinato?
        gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,tmp_p,tmp_g,tmp_t))
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
      val tmp_p = players.map(x => x._1)
      sender ! Game(game_id,game_name,game_n_players,game_status,tmp_p,tmp_g,tmp_t)
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
      val tmp_p = players.map(x => x._1)
      gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,tmp_p,tmp_g,tmp_t))
    case LeaveGame(user: UserInfo) =>
      logger.log("LeaveGame Request") 
      players = players.filterNot(elm => elm._1.uid == user.uid)
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
        val tmp_p = players.map(x => x._1)
        // Ci sono ancora giocatori nella lista quindi aggiorna lo stato
        gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,tmp_p,tmp_g,tmp_t))
      }
    case UpdatePosition(user) =>
      var player_index = (players.zipWithIndex.collect{case (g , i) if(g._1.uid == user.uid) => i}).head
      val p = new UserInfo(user.uid,user.name,user.team,user.pos,user.gold,user.keys)
      players(player_index) = players(player_index).copy(_1 = p)
      sender ! BroadcastUpdatePosition(user)
      
    case UpdateGhostsPositions =>
      //Logger.info("UpdateGhostsPositionsBroadcast")
      val tmp_g = ghosts.map(x => x._1)
      gameManagerClient ! BroadcastGhostsPositions(tmp_g)
      context.system.scheduler.scheduleOnce(500 millis, self, UpdateGhostsPositions)
    
    case GhostPositionUpdate(uid, point,mood) =>
      var ghost_index = (ghosts.zipWithIndex.collect{case (g , i) if(g._1.uid == uid) => i}).head
      var ghost_mood = mood
      var ghost_point = point
      for (i <- 0 to traps.size-1) {
        var distance = point.distanceFrom(traps(i).pos)
        if (traps(i).status == TrapStatus.IDLE && distance <= trap_radius) {
          /* La trappola traps(i) ha catturato un fantasma!
           * Settiamo il fantasma come TRAPPED, lo spostiamo forzatamente
           * dentro la trappola e iniziamo a contare 10 secondi per poi 
           * toglierla e liberarlo. Dobbiamo anche dire a tutti i client 
           * che la trappola è piena */
          ghost_mood = GhostMood.TRAPPED
          ghost_point = traps(i).pos
          ghosts(ghost_index)._2 ! GhostTrapped(ghost_point)
          traps(i).status = TrapStatus.ACTIVE
          traps(i).trapped_ghost_uid = uid
          gameManagerClient ! BroadcastTrapActivated(traps(i).getTrapInfo)
          removeTrapScheduler(traps(i).uid)
        }
      }
      var g = new GhostInfo(ghosts(ghost_index)._1.uid,ghosts(ghost_index)._1.level,ghost_mood,ghost_point)
      ghosts(ghost_index) = ghosts(ghost_index).copy(_1 = g)
      
    case PlayersPositions =>
      val tmp_p = players.map(x => x._1)
      sender ! Players(tmp_p)
    case CheckPaused =>
      if (game_status == StatusGame.PAUSED) {
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
          val tmp_p = players.map(x => x._1)
          gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,tmp_p,tmp_g,tmp_t))
          scheduler()
        } else {
          game_status = previous_game_status
          ghosts.map {ghost =>
            if (ghost._1.mood != GhostMood.TRAPPED) ghost._2 ! GhostStart
          }
          val tmp_g = ghosts.map(x => x._1)
          val tmp_t = treasures.map(x => x._1)
          val tmp_p = players.map(x => x._1)
          gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,tmp_p,tmp_g,tmp_t))
        }
        
      }
    case SetTrapRequest(user) => 
      /* Il GMB ha ricevuto la richiesta del client di mettere una trappola,
       * per controllare che il client sia consistente con il suo attore, 
       * spediamo la richiesta all'attore player e se potrà farlo sarà lui a dire "NewTrap" */
      players.filter(_._1.uid == user.uid).head._2 ! SetTrap(players.filter(_._1.uid == user.uid).head._1.gold,players.filter(_._1.uid == user.uid).head._1.pos)
      
    case NewTrap(uid,gold,pos) =>
      /* L'attore Player ci ha detto di mettere una nuova trappola,
       * lui sa le cose, quindi la piazziamo senza fare domande */
      var player_index = (players.zipWithIndex.collect{case (g , i) if(g._1.uid == uid) => i}).head
      val p = players(player_index)._1
      p.gold setAmount(gold.getAmount)
      players(player_index) = players(player_index).copy(_1 = p)
      var trap = new Trap(pos)
      traps = traps :+ trap
      gameManagerClient ! BroadcastNewTrap(trap.getTrapInfo)
      
    case RemoveTrap(uid) =>
      /* Una trappola è scattata 10 secondi fa e ora è tempo che venga rimossa */
      var trap_index = (traps.zipWithIndex.collect{case (t, t_i) if(t.uid == uid) => t_i}).head
      gameManagerClient ! BroadcastRemoveTrap(traps(trap_index).getTrapInfo)
      /* Liberiramo il fantasma intrappolato */
      var ghost_index = (ghosts.zipWithIndex.collect{case (g , i) if(g._1.uid == traps(trap_index).trapped_ghost_uid) => i}).head
      var g = new GhostInfo(ghosts(ghost_index)._1.uid,ghosts(ghost_index)._1.level,GhostMood.CALM,ghosts(ghost_index)._1.pos)
      ghosts(ghost_index) = ghosts(ghost_index).copy(_1 = g)
      ghosts(ghost_index)._2 ! GhostReleased
      traps = traps.filterNot {_.uid == uid }
  }
  
  def newGame () = {
      //...inizializza attori partita
    var width = ConfigFactory.load().getDouble("space_width")
    var height = ConfigFactory.load().getDouble("space_height")
    
    // inizializziamo i parametri di partita
    trap_radius = ConfigFactory.load().getDouble("trap_radius")

    var polygon = new Polygon(List(Point(0,0),Point(0,height),Point(width,0),Point(width,height)))
    
    val n_treasures_and_ghosts = 8
    val n_free_ghosts = game_n_players + 1
    
    var spaces = UtilFunctions.createSpaces(n_treasures_and_ghosts)
    var position_treasure = new Array[Point](n_treasures_and_ghosts)
    var position_ghosts = new Array[Point](n_treasures_and_ghosts)
    var free_position_ghosts = new Array[Point](n_free_ghosts)
    
    var position_players = new Array[Point](game_n_players)
    position_players = UtilFunctions.randomPositionsInSpace(spaces(spaces.length - 1), n_treasures_and_ghosts-1)
    
    for(i <- 0 to game_n_players-1) {
      val user = players(i)._1
      val p = new UserInfo(user.uid,user.name,user.team,Point(position_players(i).x,position_players(i).y),user.gold,user.keys)
      val player_actor = context.actorOf(Props(new Player(user.uid,user.name,user.team,polygon)), name = user.uid)
      players(i) = Tuple2(p,player_actor)
      player_actor ! UpdatePlayerPos(Point(position_players(i).x,position_players(i).y))
    }
    
    val rnd_key = new Random()
    
    var n_keys = rnd_key.nextInt(game_n_players/2)
    var keys_loot = MutableList[Key]()
    var keys_needed = MutableList[Key]()
    
    for (i <- 0 to n_keys) {
      
      if (rnd_key.nextInt(2) == 1) {
        //Creare chiave
        var key = new Key(randomString(8))
        keys_loot = keys_loot :+ key
        keys_needed = keys_needed :+ key
      }
    }
    
    for(i <- 0 to n_treasures_and_ghosts-1){
      
      // Creazione tesori
      position_treasure(i) = UtilFunctions.randomPositionInSpace(spaces(i))
      logger.log("Treasure[" + i + "] position: ("+ position_treasure(i).x +","+ position_treasure(i).y +")")
      
      var treasure_id = randomString(8)
      
      val rnd = new Random()
      var gold = new Gold(rnd.nextInt(150)+100)
      var pos_t = new Point (position_treasure(i).x,position_treasure(i).y)
      var treasure_info = new TreasureInfo(treasure_id,0,pos_t)
      
      //Random se contiene una chiave
      var rnd_loot_key = rnd.nextInt(2)
      //Random se ha bisogno di una chiave
      var rnd_need_key = rnd.nextInt(2)
      
      var key = new Key("")
      var need_key = new Key ("")
      
      if (rnd_loot_key == 1 && keys_loot.size != 0) {
        key = keys_loot(0)
        keys_loot = keys_loot.filterNot(_.getKeyUID == key.getKeyUID)
      }
      
      var tmp_key_need = MutableList[Key]()
      
      for (i <- 0 to keys_needed.size-1) {
        if (keys_needed(i).getKeyUID != key.getKeyUID) {
          tmp_key_need = tmp_key_need :+ keys_needed(i)
        }
      }
        
      if (rnd_need_key == 1 && tmp_key_need.size != 0) {
        need_key = tmp_key_need(0)
        keys_needed = keys_needed.filterNot(_.getKeyUID == need_key.getKeyUID)
      }
        
      val treasure = context.actorOf(Props(new Treasure(treasure_id,pos_t,Tuple2(key,gold),Tuple2(rnd_need_key == 1,need_key))), name = treasure_id)
      treasures = treasures :+ Tuple2(treasure_info,treasure)
      
      position_ghosts(i) = UtilFunctions.randomPositionAroundPoint(position_treasure(i))
      logger.log("Ghost[" + i + "] position: ("+ position_ghosts(i).x +","+ position_ghosts(i).y +")")
      
      // Creazione fantasmi a guardia dei tesori
      var ghost_id = randomString(8)
      var p_g = new Point (position_ghosts(i).x,position_ghosts(i).y)
      val g_level = rnd.nextInt(2)+1
      val ghost = context.actorOf(Props(new Ghost(ghost_id,polygon,p_g,g_level,treasure,pos_t)), name = ghost_id)
      var ghost_info = new GhostInfo(ghost_id,g_level,GhostMood.CALM,p_g)
      ghosts = ghosts :+ Tuple2(ghost_info,ghost)
      
    }
    
    for(i <- 0 to n_free_ghosts-1){ //l'ultimo space è dei giocatori e non ha fantasmi
      free_position_ghosts(i) = UtilFunctions.randomPositionInSpace(spaces(i))
      logger.log("Free Ghost[" + i + "] position: ("+ free_position_ghosts(i).x +","+ free_position_ghosts(i).y +")")
      
      // Fantasmi liberi di girare per tutta l'area
      val rnd = new Random()
      var free_ghost_id = randomString(8)
      var free_p_g = new Point (free_position_ghosts(i).x,free_position_ghosts(i).y)
      val n_treasure = rnd.nextInt(treasures.size)
      val free_ghost = context.actorOf(Props(new Ghost(free_ghost_id,polygon,free_p_g,3,treasures(n_treasure)._2,treasures(n_treasure)._1.pos)), name = free_ghost_id)
      var free_ghost_info = new GhostInfo(free_ghost_id,3,GhostMood.CALM,free_p_g)
      ghosts = ghosts :+ Tuple2(free_ghost_info,free_ghost)
    }
    
    game_status = StatusGame.STARTED
    val tmp_g = ghosts.map(x => x._1)
    val tmp_t = treasures.map(x => x._1)
    val tmp_p = players.map(x => x._1)
    gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,tmp_p,tmp_g,tmp_t))
    for (i <- 0 to ghosts.size-1) {
      ghosts(i)._2 ! GhostStart
    }
      
    context.system.scheduler.scheduleOnce(500 millis, self, UpdateGhostsPositions)
    
  }
  
  def selectTeam() = {
    val rnd = new Random()
    var rnd_team = rnd.nextInt(2)
    //Restituisce il numero di giocatori che appartengono già a quel tipo appena selezionato
    var count_rnd_team = players.count(_._1.team == rnd_team)
                  
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
  
  def removeTrapScheduler(uid:String) = {
     context.system.scheduler.scheduleOnce(10000 millis, self, RemoveTrap(uid))
     
  }
}
