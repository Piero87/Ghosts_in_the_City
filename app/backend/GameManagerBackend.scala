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
  var players: MutableList[Tuple2[PlayerInfo, ActorRef]] = MutableList()
  var ghosts: MutableList[Tuple2[GhostInfo, ActorRef]] = MutableList()
  var treasures: MutableList[Tuple2[TreasureInfo, ActorRef]] = MutableList()
  var traps: MutableList[Trap] = MutableList()
  
  var game_name = ""
  var game_id = java.util.UUID.randomUUID.toString
  var game_n_players = 0
  var game_status = StatusGame.WAITING
  var previous_game_status = -1
  
  var paused_players:MutableList[Tuple2[String, Long]] = MutableList()
  val icon_size = ConfigFactory.load().getDouble("icon_size")
  val initial_gold = ConfigFactory.load().getDouble("initial_gold").toInt
  val ghost_level1_damage = ConfigFactory.load().getDouble("ghost_hunger_level1")
  val ghost_level2_damage = ConfigFactory.load().getDouble("ghost_hunger_level2")
  val ghost_level3_damage = ConfigFactory.load().getDouble("ghost_hunger_level3")
  val min_treasure_gold = ConfigFactory.load().getInt("min_treasure_gold")
  val max_treasure_gold = ConfigFactory.load().getInt("max_treasure_gold")
  val ghosts_per_treasure = ConfigFactory.load().getInt("ghosts_per_treasure")
  val arena_width = ConfigFactory.load().getDouble("space_width")
  val arena_height = ConfigFactory.load().getDouble("space_height")
  val treasure_radius = ConfigFactory.load().getDouble("treasure_radius")
  val ghost_radius = ConfigFactory.load().getDouble("ghost_radius")
  val trap_radius = ConfigFactory.load().getDouble("trap_radius")
  val canvas = new Polygon(List(new Point(0, 0), new Point(0, arena_height), new Point(arena_width, 0), new Point(arena_width, arena_height)))
  
  val logger = new CustomLogger("GameManagerBackend")
  
  implicit val timeout = Timeout(5 seconds)
  implicit val ec = context.dispatcher
  
  def receive = {
    case NewGame(name,n_players,player,ref) =>
      var check_contains = canvas.contains(new Point(1000,500))
      logger.log("The point (1000,500) is in canvas? " + check_contains)
      logger.log("NewGame request")
      game_name = name
      game_n_players = n_players
      game_status = StatusGame.WAITING
      logger.log("GMBackend NewGame From: "+ref.toString())
      gameManagerClient = ref
      var rnd_team = selectTeam()
      val p = new PlayerInfo(player.uid,player.name,player.p_type,rnd_team,player.pos,initial_gold,List())
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
    case JoinGame(game,player,ref) =>
      logger.log("Join Request riceived")
      if (players.size < game_n_players) {
        logger.log("Join Request accepted")
        //Scegliamo un Team Random Blu o Rosso
        var rnd_team = selectTeam()
        
        val p = new PlayerInfo(player.uid,player.name,player.p_type,rnd_team,player.pos,initial_gold,List())
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
    case ResumeGame(gameid: String, player: PlayerInfo, ref: ActorRef) =>
      logger.log("ResumeGame Request")
      paused_players = paused_players.filterNot(elm => elm._1 == player.uid)
      // Svegliamo il giocatore che è appena tornato!
      val tmp_g = ghosts.map(x => x._1)
      val tmp_t = treasures.map(x => x._1)
      val tmp_p = players.map(x => x._1)
      sender ! Game(game_id,game_name,game_n_players,game_status,tmp_p,tmp_g,tmp_t)
    case PauseGame(p_uid) =>
      logger.log("PauseGame Request")
      paused_players = paused_players :+ Tuple2(p_uid, System.currentTimeMillis())
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
    case LeaveGame(p_uid) =>
      logger.log("LeaveGame Request") 
      players = players.filterNot(elm => elm._1.uid == p_uid)
      sender ! Success
     
      // Se non abbiamo più giocatori dobbiamo dire al GameManager Client  di uccidersi
      if (players.size == 0 || game_status == StatusGame.STARTED) {
        
        var all_player_info = players.map(x => x._1).toList
        gameManagerClient ! BroadcastVictoryResponse(Team.NO_ENOUGH_PLAYER,all_player_info)
        
        self ! Finish
      } else {
        val tmp_g = ghosts.map(x => x._1)
        val tmp_t = treasures.map(x => x._1)
        val tmp_p = players.map(x => x._1)
        // Ci sono ancora giocatori nella lista quindi aggiorna lo stato
        gameManagerClient ! GameStatusBroadcast(Game(game_id,game_name,game_n_players,game_status,tmp_p,tmp_g,tmp_t))
      }
    case UpdatePosition(player) =>
      if (canvas.contains(player.pos)) {
        var player_index = (players.zipWithIndex.collect{case (g , i) if(g._1.uid == player.uid) => i}).head
        var u_tmp = players(player_index)._1
        var player_info = new PlayerInfo(u_tmp.uid,u_tmp.name,u_tmp.p_type,u_tmp.team,player.pos,u_tmp.gold,u_tmp.keys)
        players(player_index) = players(player_index).copy(_1 = player_info)
        sender ! BroadcastUpdatePosition(player)
      } else {
        gameManagerClient ! MessageCode(player.uid,MsgCodes.OUT_OF_AREA,"")
      }
        
      
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
              /* Invio le info di terminazione partita a causa di un utente 
               * che è uscito a causa di un disservizio e non è tornato
               */
              players = players.filterNot(elm => elm._1.uid == player._1)
              var all_player_info = players.map(x => x._1).toList
              gameManagerClient ! BroadcastVictoryResponse(Team.NO_ENOUGH_PLAYER,all_player_info)
        
              self ! Finish
              
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
    case SetTrapRequest(p_uid) => 
      /* Il GMB ha ricevuto la richiesta del client di mettere una trappola,
       * per controllare che il client sia consistente con il suo attore, 
       * spediamo la richiesta all'attore player e se potrà farlo sarà lui a dire "NewTrap" */
      var player = players.filter(_._1.uid == p_uid).head
      player._2 ! SetTrap(player._1.gold,player._1.pos)
      
    case NewTrap(uid,gold,pos) =>
      /* L'attore Player ci ha detto di mettere una nuova trappola,
       * lui sa le cose, quindi la piazziamo senza fare domande */
      var player_index = (players.zipWithIndex.collect{case (g , i) if(g._1.uid == uid) => i}).head
      var u_tmp = players(player_index)._1
      var player_info = new PlayerInfo(u_tmp.uid,u_tmp.name,u_tmp.p_type,u_tmp.team,u_tmp.pos,gold,u_tmp.keys)
      players(player_index) = players(player_index).copy(_1 = player_info)
      var trap = new Trap(pos)
      traps = traps :+ trap
      gameManagerClient ! BroadcastNewTrap(trap.getTrapInfo)
      gameManagerClient ! UpdatePlayerInfo(player_info)
      
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
        var player_keys = u_tmp.keys
        
        var gold_found_message = false
        var key_found_message = false
        
        for (t <- t_opened) {
          
          //Se è diverso da stringa vuota vuol dire che ho usato una chiave per aprirlo e la rimuovo dal giocatore
          if (t._5 != "")
          {
            player_keys = player_keys.filter(_.getKeyUID != t._5)
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
          var player_info = new PlayerInfo(u_tmp.uid,u_tmp.name,u_tmp.p_type,u_tmp.team,u_tmp.pos,u_tmp.gold+gold_tmp,List.concat(player_keys,keys_tmp))
          players(player_index) = players(player_index).copy(_1 = player_info)
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
          
          gameManagerClient ! UpdatePlayerInfo(player_info)
          
          var remaining_closed_treasures = treasures.filter(_._1.status == 0)
          
          if (remaining_closed_treasures.size == 0) {
            //La partita è finita, tutti i tesori sono stati aperti
            var team_red_gold = ((players.filter(_._1.team == Team.RED)).map(x => x._1.gold)).sum
            var team_blue_gold = ((players.filter(_._1.team == Team.BLUE)).map(x => x._1.gold)).sum
            
            var all_player_info = players.map(x => x._1).toList
            
            if (team_red_gold > team_blue_gold) {
              //La squadra rossa ha vinto
              gameManagerClient ! BroadcastVictoryResponse(Team.RED,all_player_info)
            } else if (team_red_gold < team_blue_gold) {
              //La squadra blu ha vinto
              gameManagerClient ! BroadcastVictoryResponse(Team.BLUE,all_player_info)
            } else {
              //Pareggio
              gameManagerClient ! BroadcastVictoryResponse(Team.UNKNOWN,all_player_info)
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
    case HitPlayerRequest(uid) => 
      /* Il GMB ha ricevuto la richiesta del client di attaccare un altro giocatore,
       * per controllare che il client sia consistente con il suo attore, 
       * spediamo la richiesta all'attore player e se potrà farlo sarà lui a dire "PlayerAttacked" */
      var player = players.filter(_._1.uid == uid).head
      logger.log("Hit player request from: " + player._1.name)
      var other_players = players.filterNot(_._1.uid == uid)
      var p_actorref_list = (other_players.filter(_._1.pos.distanceFrom(player._1.pos) <= icon_size/2)).map(x => x._2).toList
      if (p_actorref_list.size != 0 ) {
        player._2 ! AttackHim(p_actorref_list.head)
      }
      
    case PlayerAttacked(uid, attacker_uid, attack_type, gold_perc_stolen, keys_stolen) =>
      
      var origin = sender
      var attacked_index = (players.zipWithIndex.collect{case (g , i) if(g._1.uid == uid) => i}).head
      var u_tmp = players(attacked_index)._1
      var attacked_keys = u_tmp.keys
      //Se il giocatore ha soldi
      if (u_tmp.gold != 0) {
        var gold_stolen_double = u_tmp.gold * gold_perc_stolen
        var gold_stolen = gold_stolen_double.toInt
        var gold_remain = u_tmp.gold - gold_stolen
        if (attack_type == MsgCodes.PARANORMAL_ATTACK){
          //Mando al fantasmi il numero di soldi rubati
          origin ! gold_stolen
        } else {
          var attacker_index = (players.zipWithIndex.collect{case (g , i) if(g._1.uid == attacker_uid) => i}).head
          var att_tmp = players(attacker_index)._1
          var attacker_keys = att_tmp.keys
          if (keys_stolen == 1){
            attacked_keys = List()
            attacker_keys = List.concat(att_tmp.keys, u_tmp.keys)
          }
          var attacker_info = new PlayerInfo(att_tmp.uid,att_tmp.name,att_tmp.p_type,att_tmp.team,att_tmp.pos,att_tmp.gold+gold_stolen,attacker_keys)
          players(attacker_index) = players(attacker_index).copy(_1 = attacker_info)
          gameManagerClient ! UpdatePlayerInfo(attacker_info)
          logger.log("Player " + u_tmp.name + " attacked from: " + att_tmp)
        }

        var attacked_info = new PlayerInfo(u_tmp.uid,u_tmp.name,u_tmp.p_type,u_tmp.team,u_tmp.pos,gold_remain,attacked_keys)
        players(attacked_index) = players(attacked_index).copy(_1 = attacked_info)
        gameManagerClient ! MessageCode(uid, attack_type,gold_stolen.toString())
        gameManagerClient ! UpdatePlayerInfo(attacked_info)
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
    // inizializziamo i parametri di partita
    var n_treasures_and_ghosts = game_n_players*2
    var n_free_ghosts = game_n_players + 1
    
    try {
      
      var spaces = UtilFunctions.createSpaces(n_treasures_and_ghosts, canvas)
      var position_players = new Array[Point](game_n_players)
      position_players = UtilFunctions.randomPositionsInSpace(spaces(spaces.length - 1), canvas, n_treasures_and_ghosts-1)
    
      for(i <- 0 to game_n_players-1) {
        val player = players(i)._1
        val p = new PlayerInfo(player.uid,player.name,player.p_type,player.team,Point(position_players(i).latitude,position_players(i).longitude),player.gold,player.keys)
        val player_actor = context.actorOf(Props(new Player(player.uid,player.name,player.team,self)), name = player.uid)
        players(i) = Tuple2(p,player_actor)
        player_actor ! UpdatePlayerPos(Point(position_players(i).latitude,position_players(i).longitude))
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
        
        var treasure_position : Point = UtilFunctions.randomPositionInSpace(spaces_shuffled(i), canvas)
        logger.log("Treasure[" + i + "] position: ("+ treasure_position.latitude +","+ treasure_position.longitude +")")
        
        var treasure_id = randomString(8)
        
        val rnd = new Random()
        var gold = rnd.nextInt(max_treasure_gold-min_treasure_gold)+min_treasure_gold
        var pos_t = new Point (treasure_position.latitude,treasure_position.longitude)
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
        
        for(i <- 1 to ghosts_per_treasure){
          var ghost_postion : Point = UtilFunctions.randomPositionAroundPoint(treasure_position, treasure_radius, canvas)
          logger.log("Ghost[" + i + "] position: ("+ ghost_postion.latitude +","+ ghost_postion.longitude +")")
          
          // Creazione dei due fantasmi a guardia dei tesori
          // Fantasma 1
          var ghost_id = randomString(8)
          var p_g = new Point (ghost_postion.latitude,ghost_postion.longitude)
          val g_level = rnd.nextInt(2)+1
          val ghost = context.actorOf(Props(new Ghost(ghost_id,canvas,p_g,g_level,treasures.last._2,pos_t,treasure_id)), name = ghost_id)
          var ghost_info = new GhostInfo(ghost_id,g_level,GhostMood.CALM,p_g)
          ghosts = ghosts :+ Tuple2(ghost_info,ghost)
        }
        
      }
      
      for(i <- 0 to n_free_ghosts-1){ //l'ultimo space è dei giocatori e non ha fantasmi
        var free_ghost_postion = UtilFunctions.randomPositionInSpace(spaces(i), canvas)
        logger.log("Free Ghost[" + i + "] position: ("+ free_ghost_postion.latitude +","+ free_ghost_postion.longitude +")")
        
        // Fantasmi liberi di girare per tutta l'area
        val rnd = new Random()
        var free_ghost_id = randomString(8)
        var free_p_g = new Point (free_ghost_postion.latitude, free_ghost_postion.longitude)
        val n_treasure = rnd.nextInt(treasures.size)
        val free_ghost = context.actorOf(Props(new Ghost(free_ghost_id,canvas,free_p_g,3,treasures(n_treasure)._2,treasures(n_treasure)._1.pos,treasures(n_treasure)._1.uid)), name = free_ghost_id)
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
    } catch {
      case e: PointOutOfPolygonException => logger.log("ERROR! Position error " + e.getMessage)
      case e: Exception => logger.log("ERROR! Something went wrong...")
    }
    
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
