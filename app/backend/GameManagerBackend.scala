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
  var icon_size = ConfigFactory.load().getDouble("icon_size")
  var initial_gold = ConfigFactory.load().getDouble("initial_gold").toInt
  var ghost_level1_damage = ConfigFactory.load().getDouble("ghost_hunger_level1")
  var ghost_level2_damage = ConfigFactory.load().getDouble("ghost_hunger_level2")
  var ghost_level3_damage = ConfigFactory.load().getDouble("ghost_hunger_level3")
  
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
      val p = new UserInfo(user.uid,user.name,rnd_team,user.pos,initial_gold,List())
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
        
        val p = new UserInfo(user.uid,user.name,rnd_team,user.pos,initial_gold,List())
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
        
        var all_user_info = players.map(x => x._1).toList
        gameManagerClient ! BroadcastVictoryResponse(Team.NO_ENOUGH_PLAYER,all_user_info)
        
        self ! Finish
      } else {
        val tmp_g = ghosts.map(x => x._1)
        val tmp_t = treasures.map(x => x._1)
        val tmp_p = players.map(x => x._1)
        // Ci sono ancora giocatori nella lista quindi aggiorna lo stato
        gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,tmp_p,tmp_g,tmp_t))
      }
    case UpdatePosition(user) =>
      var player_index = (players.zipWithIndex.collect{case (g , i) if(g._1.uid == user.uid) => i}).head
      var u_tmp = players(player_index)._1
      var user_info = new UserInfo(u_tmp.uid,u_tmp.name,u_tmp.team,user.pos,u_tmp.gold,u_tmp.keys)
      players(player_index) = players(player_index).copy(_1 = user_info)
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
      
    case PlayersInfo =>
      sender ! Players(players)
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
      var player = players.filter(_._1.uid == user.uid).head
      player._2 ! SetTrap(player._1.gold,player._1.pos)
      
    case NewTrap(uid,gold,pos) =>
      /* L'attore Player ci ha detto di mettere una nuova trappola,
       * lui sa le cose, quindi la piazziamo senza fare domande */
      var player_index = (players.zipWithIndex.collect{case (g , i) if(g._1.uid == uid) => i}).head
      var u_tmp = players(player_index)._1
      var user_info = new UserInfo(u_tmp.uid,u_tmp.name,u_tmp.team,u_tmp.pos,gold,u_tmp.keys)
      players(player_index) = players(player_index).copy(_1 = user_info)
      var trap = new Trap(pos)
      traps = traps :+ trap
      gameManagerClient ! BroadcastNewTrap(trap.getTrapInfo)
      gameManagerClient ! UpdateUserInfo(user_info)
      
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
      
    case MessageCode(uid,code,option) =>
      gameManagerClient ! MessageCode(uid,code,option)
      
    case OpenTreasureRequest(uid) =>
      var player = players.filter(_._1.uid == uid).head
      var t_actorref_list = (treasures.filter(_._1.pos.distanceFrom(player._1.pos) <= icon_size/2)).map(x => x._2).toList
      if (t_actorref_list.size != 0 ) {
        player._2 ! OpenTreasure(t_actorref_list,player._1)
      } else {
        gameManagerClient ! MessageCode(uid, MsgCodes.NO_T_NEAR_YOU,"0")
      }
      
    case TreasureResponse(uid_p,results) =>
      //Controllo se abbiamo aperto uno o più tesori
      var t_opened = results.filter(_._1 == MsgCodes.T_SUCCESS_OPENED)
      var t_needs_key = results.filter(_._1 == MsgCodes.T_NEEDS_KEY)
      
      if (t_opened.size != 0) {
        
        var player_index = (players.zipWithIndex.collect{case (g , i) if(g._1.uid == uid_p) => i}).head
        var u_tmp = players(player_index)._1
        var check_empty = true
        var keys_tmp: MutableList[Key] = MutableList()
        var gold_tmp = 0
        var user_keys = u_tmp.keys
        
        var gold_found_message = false
        var key_found_message = false
        
        for (t <- t_opened) {
          
          //Se è diverso da stringa vuota vuol dire che ho usato una chiave per aprirlo e la rimuovo dal giocatore
          if (t._5 != "")
          {
            user_keys = user_keys.filter(_.getKeyUID != t._5)
          }
          
          if (t._2.getKeyUID != "") {
            check_empty = false
            keys_tmp = keys_tmp :+ t._2
            key_found_message = true
          }
          
          if (t._3 != 0) {
            check_empty = false
            gold_tmp = gold_tmp + t._3
            gold_found_message = true
          }
          
          //Controllo se il tesoro è stato aperto adesso, perchè devo cambiare il suo status in TreasureInfo
          if (!check_empty)
          {
            var t_index = (treasures.zipWithIndex.collect{case (g , i) if(g._1.uid == t._4) => i}).head
            var t_tmp = treasures(t_index)._1
            var t_info = new TreasureInfo(t_tmp.uid,1,t_tmp.pos)
            treasures(t_index) = treasures(t_index).copy(_1 = t_info)
          }
        }
        
        if (check_empty) {
          logger.log("New Message! code: " + MsgCodes.T_EMPTY + " option: 0 to: " + uid_p + " game_id:" + game_id)
          gameManagerClient ! MessageCode(uid_p, MsgCodes.T_EMPTY,"0")
        } else {
          var user_info = new UserInfo(u_tmp.uid,u_tmp.name,u_tmp.team,u_tmp.pos,u_tmp.gold+gold_tmp,List.concat(user_keys,keys_tmp))
          players(player_index) = players(player_index).copy(_1 = user_info)
          val tmp_t_info = treasures.map(x => x._1)
          gameManagerClient ! BroadcastUpdateTreasure(tmp_t_info)
          if (gold_found_message && key_found_message)
          {
            logger.log("New Message! code: " + MsgCodes.K_G_FOUND + " option: " + gold_tmp.toString() + " to: " + uid_p + " game_id:" + game_id)
            gameManagerClient ! MessageCode(uid_p, MsgCodes.K_G_FOUND,gold_tmp.toString())
          } else if (key_found_message) {
            logger.log("New Message! code: " + MsgCodes.KEY_FOUND + " option: " + gold_tmp.toString() + " to: " + uid_p + " game_id:" + game_id)
            gameManagerClient ! MessageCode(uid_p, MsgCodes.KEY_FOUND,gold_tmp.toString())
          } else if (gold_found_message) {
            logger.log("New Message! code: " + MsgCodes.GOLD_FOUND + " option: " + gold_tmp.toString() + " to: " + uid_p + " game_id:" + game_id)
            gameManagerClient ! MessageCode(uid_p, MsgCodes.GOLD_FOUND,gold_tmp.toString())
          }
          
          gameManagerClient ! UpdateUserInfo(user_info)
          
          var remaining_closed_treasures = treasures.filter(_._1.status == 0)
          
          if (remaining_closed_treasures.size == 0) {
            //La partita è finita, tutti i tesori sono stati aperti
            var team_red_gold = ((players.filter(_._1.team == Team.RED)).map(x => x._1.gold)).sum
            var team_blue_gold = ((players.filter(_._1.team == Team.BLUE)).map(x => x._1.gold)).sum
            
            var all_user_info = players.map(x => x._1).toList
            
            if (team_red_gold > team_blue_gold) {
              //La squadra rossa ha vinto
              gameManagerClient ! BroadcastVictoryResponse(Team.RED,all_user_info)
            } else if (team_red_gold < team_blue_gold) {
              //La squadra blu ha vinto
              gameManagerClient ! BroadcastVictoryResponse(Team.BLUE,all_user_info)
            } else {
              //Pareggio
              gameManagerClient ! BroadcastVictoryResponse(Team.UNKNOWN,all_user_info)
            }
            
            context.system.scheduler.scheduleOnce(1000 millis, self, Finish)
            
          }
          
        }
      } else if (t_needs_key.size != 0) {
        gameManagerClient ! MessageCode(uid_p, MsgCodes.T_NEEDS_KEY,"0")
      }
    case UpdateTreasure(uid,status) =>
      var t_index = (treasures.zipWithIndex.collect{case (g , i) if(g._1.uid == uid) => i}).head
      var t_tmp = treasures(t_index)._1
      //Invio il Broadcast solo se è cambiato il suo status
      if (t_tmp.status != status) {
        var t_info = new TreasureInfo(t_tmp.uid,0,t_tmp.pos)
        treasures(t_index) = treasures(t_index).copy(_1 = t_info)
        val tmp_t_info = treasures.map(x => x._1)
        gameManagerClient ! BroadcastUpdateTreasure(tmp_t_info)
      }
    case PlayerAttacked(uid, level) =>
      
      var origin = sender
      var player_index = (players.zipWithIndex.collect{case (g , i) if(g._1.uid == uid) => i}).head
      var u_tmp = players(player_index)._1
      //Se il giocatore ha soldi
      if (u_tmp.gold != 0) {
        var gold_stolen_double = 0.0
        level match {
          case 1 => {
            gold_stolen_double = u_tmp.gold * ghost_level1_damage
          }
          case 2 => {
            gold_stolen_double = u_tmp.gold * ghost_level2_damage
          }
          case 3 => {
            gold_stolen_double = u_tmp.gold * ghost_level3_damage
          }
        }
        var gold_stolen = gold_stolen_double.toInt
        var user_info = new UserInfo(u_tmp.uid,u_tmp.name,u_tmp.team,u_tmp.pos,u_tmp.gold-gold_stolen,u_tmp.keys)
        players(player_index) = players(player_index).copy(_1 = user_info)
        gameManagerClient ! MessageCode(uid, MsgCodes.PARANORMAL_ATTACK,gold_stolen.toString())
        gameManagerClient ! UpdateUserInfo(user_info)
        //Mando al fantasmi il numero di soldi rubati
        origin ! gold_stolen
      }
    case Finish =>
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
  }
  
  //Metodo per stampare il contenuto delle liste
  def printList(args: TraversableOnce[_]): Unit = {
    args.foreach(println)
  }
  
  def newGame () = {
      //...inizializza attori partita
    var width = ConfigFactory.load().getDouble("space_width")
    var height = ConfigFactory.load().getDouble("space_height")
    
    // inizializziamo i parametri di partita
    trap_radius = ConfigFactory.load().getDouble("trap_radius")

    var polygon = new Polygon(List(Point(0,0),Point(0,height),Point(width,0),Point(width,height)))
    
    var n_treasures_and_ghosts = game_n_players*2
    var n_free_ghosts = game_n_players + 1
    
    var spaces = UtilFunctions.createSpaces(n_treasures_and_ghosts)
    var position_treasure = new Array[Point](n_treasures_and_ghosts)
    var position_ghosts = new Array[Point](n_treasures_and_ghosts)
    var free_position_ghosts = new Array[Point](n_free_ghosts)
    
    var position_players = new Array[Point](game_n_players)
    position_players = UtilFunctions.randomPositionsInSpace(spaces(spaces.length - 1), n_treasures_and_ghosts-1)
    
    for(i <- 0 to game_n_players-1) {
      val user = players(i)._1
      val p = new UserInfo(user.uid,user.name,user.team,Point(position_players(i).x,position_players(i).y),user.gold,user.keys)
      val player_actor = context.actorOf(Props(new Player(user.uid,user.name,user.team,polygon,self)), name = user.uid)
      players(i) = Tuple2(p,player_actor)
      player_actor ! UpdatePlayerPos(Point(position_players(i).x,position_players(i).y))
    }
    
    val rnd_key = new Random()
    
    var n_treasures_tmp = n_treasures_and_ghosts
    var n_keys_tmp = rnd_key.nextInt(n_treasures_and_ghosts/2)
    var n_treasures_closed_tmp = n_keys_tmp
    var index = 0
    
    var keys_container = MutableList[Key]()
    
    for (i <- 0 to n_keys_tmp-1) {
      var key = new Key(randomString(8))
      keys_container = keys_container :+ key
    }
    
    var empty_key = new Key("")
          
    var spaces_shuffled = (util.Random.shuffle(spaces.toList)).toArray
    
    for(i <- 0 to n_treasures_and_ghosts-1){
      
      // Creazione tesori
      position_treasure(i) = UtilFunctions.randomPositionInSpace(spaces_shuffled(i))
      logger.log("Treasure[" + i + "] position: ("+ position_treasure(i).x +","+ position_treasure(i).y +")")
      
      var treasure_id = randomString(8)
      
      val rnd = new Random()
      var gold = rnd.nextInt(150)+100
      var pos_t = new Point (position_treasure(i).x,position_treasure(i).y)
      var treasure_info = new TreasureInfo(treasure_id,0,pos_t)
      
      if (n_keys_tmp != 0) {
        
        var tmp_key = keys_container(index)
        var treasure = context.actorOf(Props(new Treasure(treasure_id,pos_t,Tuple2(tmp_key,gold),Tuple2(false,empty_key),self)), name = treasure_id)
        treasures = treasures :+ Tuple2(treasure_info,treasure)
        n_keys_tmp = n_keys_tmp-1
        
        if (keys_container.size == index+1)
        {
          index = 0
        } else {
          index = index +1
        }
        
      } else if (n_treasures_closed_tmp != 0)
      {
        var tmp_key = keys_container(index)
        var treasure = context.actorOf(Props(new Treasure(treasure_id,pos_t,Tuple2(empty_key,gold),Tuple2(true,tmp_key),self)), name = treasure_id)
        treasures = treasures :+ Tuple2(treasure_info,treasure)
        n_treasures_closed_tmp = n_treasures_closed_tmp-1
        
        if (keys_container.size == index+1)
        {
          index = 0
        } else {
          index = index +1
        }
        
      } else {
        var treasure = context.actorOf(Props(new Treasure(treasure_id,pos_t,Tuple2(empty_key,gold),Tuple2(false,empty_key),self)), name = treasure_id)
        treasures = treasures :+ Tuple2(treasure_info,treasure)
      }
      
      position_ghosts(i) = UtilFunctions.randomPositionAroundPoint(position_treasure(i))
      logger.log("Ghost[" + i + "] position: ("+ position_ghosts(i).x +","+ position_ghosts(i).y +")")
      
      // Creazione fantasmi a guardia dei tesori
      var ghost_id = randomString(8)
      var p_g = new Point (position_ghosts(i).x,position_ghosts(i).y)
      val g_level = rnd.nextInt(2)+1
      val ghost = context.actorOf(Props(new Ghost(ghost_id,polygon,p_g,g_level,treasures.last._2,pos_t,treasure_id)), name = ghost_id)
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
      val free_ghost = context.actorOf(Props(new Ghost(free_ghost_id,polygon,free_p_g,3,treasures(n_treasure)._2,treasures(n_treasure)._1.pos,treasures(n_treasure)._1.uid)), name = free_ghost_id)
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
