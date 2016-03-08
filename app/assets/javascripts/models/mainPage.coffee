#
# The main page.
#
# This class handles most of the player interactions with the buttons/menus/forms on the page, as well as manages
# the WebSocket connection.	It delegates to other classes to manage everything else.
#
define ["knockout", "gps", "gameClientEngine"], (ko, Gps, GameClientEngine) ->
	class MainPageModel
		constructor: () ->
			
			@music = ko.observable()
			@sounds = ko.observable()
			@debug = ko.observable()
			
			@music("on")
			@sounds("on")
			@debug("off")
						
			# Player data
			@playername = ko.observable()
			@playeruid = ko.observable()
			@playergold = ko.observable()
			@playerkeys = ko.observable()
			
			@playergold(0)
			@playerkeys(0)
			
			# Game data
			@gameready = ko.observable(false)
			@gamestarted = ko.observable(false)
			@gamepaused = ko.observable(false)
			@gameended= ko.observable(false)
			@gameid = ko.observable()
			@gamename = ko.observable()
			@gamecreator = ko.observable()
			@gametime = ko.observable()
			@gamemaxplayers = ko.observable(2)
			@gameplayersmissing = ko.observable()
			@game_team_RED = ko.observableArray()
			@game_team_BLUE = ko.observableArray()
			@game_client_engine = null
			
			# Interval to send a lot of request for available games
			@interval = null
			
			# Games list
			@gamesavailable = ko.observable(false)
			@gameslist = ko.observableArray()
			
			# The Web Socket
			@ws = null
			
			@disconnected = ko.observable(true)
			@connected = ko.observable(false)
			
			@connecting = ko.observable()
			@closing = false
			
			# Load previously player name if set
			if localStorage.playername
				@playername(localStorage.playername)
				@playeruid(localStorage.uid)
				
				@connect()
		
		# Connect
		connect: ->
			@connecting("Connecting...")
			@disconnected(null)
			
			@ws = new WebSocket(jsRoutes.controllers.Application.login(@playername(), @playeruid()).webSocketURL())
			
			# When the websocket opens
			@ws.onopen = (event) =>
				@connecting(null)
				@connected(true)
				$("#ghostbusters-song").get(0).play()
				
				@game_client_engine = new GameClientEngine(@playeruid(), @ws)
				
				if localStorage.gameid
					@gameid(localStorage.gameid)
					@resumeGame()
				else
					# Setting the interval for refresh games list
					callback = @gamesList.bind(this)
					@interval = setInterval(callback, 1000)
				
			# When the websocket closes
			@ws.onclose = (event) =>
				# Handle the reconnection in case of errors
				if (!event.wasClean && ! self.closing)
					@connected(false)
					@connect()
					@connecting("Reconnecting ...")
				else
					# Destroy everything and clean all
					@disconnected(true)
					@connected(false)
					@closing = false
					localStorage.removeItem("uid")
					localStorage.removeItem("playername")
					localStorage.removeItem("gameid")
			
			# Handle the stream
			@ws.onmessage = (event) =>
				json = JSON.parse(event.data)
				if json.event == "player-positions"
					console.log('player Position Received!')
					# Update all the markers on the map
					#@map.updateMarkers(json.positions.features)
					
				else if json.event == "games_list"
					#console.log('Games list received!')
					@gameslist.removeAll()
					if json.list.length > 0
						@gamesavailable(true)
						for game in json.list
							game_details = game.name.split "__"
							gamecreator = game_details[0].split("_").join(" ")
							date = new Date(parseInt( game_details[1], 10 ));
							hours = date.getHours()
							minutes = "0" + date.getMinutes()
							seconds = "0" + date.getSeconds()
							gametime = hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2)
							game.name = "Mission created by " + gamecreator + " at " + gametime
							@gameslist.push(game)
					else
						@gamesavailable(false)
						
				else if json.event == "game_ready"
					console.log('Ready!')
					if (@interval)
						clearInterval(@interval)
						@interval = null
					@gameready(true)
					@gameid(json.game.id)
					localStorage.setItem("gameid", @gameid())
					@setGameName(json.game.name)
					@gamemaxplayers(json.game.n_players)
					
					@game_client_engine = new GameClientEngine(@playeruid(), @ws) if (@game_client_engine == null)
					
					$("#game-result-won").hide()
					$("#game-result-lost").hide()
					$("#game-result-draw").hide()
					
					# Update status variables
					@gameready(true)
					@gamestarted(false)
					@gameended(false)
					
					@refreshPlayerList(json)
					
				else if json.event == "game_status"
					# {event: "game_status", game: {id: [Int], name: [String], n_players: [Int], players [Array of String], status: [Int]}}
					@changeGameStatus(json.game.status)
					switch json.game.status
						when 0 # game waiting
							console.log('Ready!')
							@refreshPlayerList(json)
						when 1 # game started
							
							@setGameName(json.game.name)
							
							console.log(json)
							console.log('Fight!')
							@refreshPlayerList(json)
							@gamename(json.game.name)
							@game_client_engine.setBusters(json.game.players)
							@game_client_engine.setGhosts(json.game.ghosts)
							@game_client_engine.setTreasures(json.game.treasures)
							@game_client_engine.startGame()
							@playergold(player.gold) for player in json.game.players when player.uid == @playeruid()
						when 2 # game paused
							console.log('Hold on!')
							@game_client_engine.pauseGame()
						when 3 # game ended
							console.log 'Game Over!'
							@gamename("")
							localStorage.removeItem("gameid")
							@game_client_engine.endGame()
							@game_client_engine = null
										
				else if json.event == "update_player_position"
					if @gamestarted()
						@game_client_engine.busterMove(json.player.uid, json.player.pos.latitude, json.player.pos.longitude)
						
				else if json.event == "update_player_info"
					if @gamestarted()
						console.log(json)
						@playergold(json.player.gold)
						@playerkeys(json.player.keys.length)
						
				else if json.event == "update_ghosts_positions"
					if @gamestarted()
						@game_client_engine.ghostMove(ghost.uid, ghost.mood, ghost.pos.latitude, ghost.pos.longitude) for ghost in json.ghosts
				
				else if json.event == "update_treasures"
					console.log("Tesoro aperto!")
					console.log(json.treasures)
					if @gamestarted()
						@game_client_engine.changeTreasureStatus(treasure.uid, treasure.status) for treasure in json.treasures
				
				else if json.event == "new_trap"
					if @gamestarted()
						@game_client_engine.newTrap(json.trap.uid, json.trap.pos.latitude, json.trap.pos.longitude)
						console.log "Nuova trappola!"
						# console.log json.trap
				
				else if json.event == "active_trap"
					if @gamestarted()
						$("#trap-activated").get(0).play()
						@game_client_engine.activeTrap(json.trap.uid) if (json.trap.status == 1)
						console.log "Trappola attivata!"
						# console.log json.trap
				
				else if json.event == "remove_trap"
					if @gamestarted()
						@game_client_engine.removeTrap(json.trap.uid)
						console.log "Trappola rimossa!"
						# console.log json.trap
				
				else if json.event == "message"
					@showMessage(json.code, json.option)
					
				else if json.event == "game_results"
					# "team" [0,1,-1] : winning team
					# "players" [list of player_info] : players of the game
					player_team = -1
					
					@game_team_RED.removeAll()
					@game_team_BLUE.removeAll()
					if json.players.length > 0
						i = 0
						for player in json.players
							i = i + 1
							player.index = i
							if (player.uid == @playeruid())
								player_team = player.team
							if (player.team == 0)
								@game_team_RED.push(player)
							else if (player.team == 1)
								@game_team_BLUE.push(player)
								
					if (json.team == -1)
						$("#game-result-draw").show()
					else if (json.team == -2)
						$("#game-result-won").show()
					else if (json.team == player_team)
						$("#game-result-won").show()
					else 
						$("#game-result-lost").show()
					
		# Toggle the background music
		toggleMusic: ->
			if (@music() == "on")
				@music("off")
				document.getElementById("ghostbusters-song").volume = 0.0;
				document.getElementById("ghostbusters-theme").volume = 0.0;
			else if (@music() == "off")
				@music("on")
				document.getElementById("ghostbusters-song").volume = 0.1;
				document.getElementById("ghostbusters-theme").volume = 0.2;
		
		# Toggle the sounds effects in game
		toggleSounds: ->
			if (@sounds() == "on")
				@sounds("off")
				document.getElementById("gold-found").volume = 0.0;
				document.getElementById("keys-found").volume = 0.0;
				document.getElementById("trap-activated").volume = 0.0;
				document.getElementById("treasure-locked").volume = 0.0;
				document.getElementById("treasure-opening").volume = 0.0;
				document.getElementById("ghost-attack").volume = 0.0;
			else if (@sounds() == "off")
				@sounds("on")
				document.getElementById("gold-found").volume = 0.5;
				document.getElementById("keys-found").volume = 0.5;
				document.getElementById("trap-activated").volume = 0.5;
				document.getElementById("treasure-locked").volume = 0.5;
				document.getElementById("treasure-opening").volume = 0.5;
				document.getElementById("ghost-attack").volume = 0.5;
		
		toggleDebug: ->
			if (@debug() == "on")
				@debug("off")
				@game_client_engine.toggleDebug(false)
			else if (@debug() == "off")
				@debug("on")
				@game_client_engine.toggleDebug(true)
		
		# The player clicked connect
		submitPlayerName: ->
			name = @playername()
			name = name.replace /\s/g,''
			@playername(name)
			@playeruid(@generateUID())
			localStorage.setItem("uid", @playeruid())
			localStorage.setItem("playername", @playername())
			@connect()
		
		# New Game 
		newGame: ->
			gamename = @playername()
			gamemaxplayers = @gamemaxplayers()
			console.log("New Game")
			@ws.send(JSON.stringify
				event: "new_game"
				name: gamename
				pos:
					latitude: parseInt( 0, 10 )
					longitude: parseInt( 0, 10 )
				game_area_edge: parseInt( 0, 10 )
				n_players: parseInt( gamemaxplayers, 10 )
				game_type: "web"
			)
		
		# Games list
		gamesList: ->
			@ws.send(JSON.stringify
				event: "games_list"
				g_type: "web"
			)
		
		# Join Game 
		joinGame: (game) ->
			@ws.send(JSON.stringify
				event: "join_game"
				game: game
				pos:
					latitude: parseInt( 0, 10 )
					longitude: parseInt( 0, 10 )
			)
		
		# Resume Game
		resumeGame: ->
			@ws.send(JSON.stringify
				event: "resume_game"
				game_id: @gameid()
			)
		
		# Disconnect the ws
		disconnect: ->
			if (@interval)
				clearInterval(@interval)
				@interval = null
			@changeGameStatus(-1)
			@closing = true
			@ws.close()
		
		# Leave Game
		leaveGame: ->
			@clearGameData()
			@ws.send(JSON.stringify
				event: "leave_game"
			)
		
		playAgain: ->
			@clearGameData()
			window.location.reload(true)
		
		clearGameData: ->
			@changeGameStatus(-1)
			@gamename("")
			localStorage.removeItem("gameid")
			@gamemaxplayers(2)
			@game_team_RED.removeAll()
			@game_team_BLUE.removeAll()
			
			callback = @gamesList.bind(this)
			@interval = setInterval(callback, 1000)
		
		changeGameStatus: (s) ->
			status = s
			switch status
				when -1 # game leaved
					@gameready(false)
					@gamestarted(false)
					@gamepaused(false)
					@gameended(false)
					$("#ghostbusters-song").get(0).currentTime = 0
					$("#ghostbusters-song").get(0).play()
					$("#ghostbusters-theme").get(0).pause()
				when 0 # game ready - wait for other players
					@gameready(true)
					@gamestarted(false)
					@gamepaused(false)
					@gameended(false)
				when 1 # game started
					@gameready(false)
					@gamestarted(true)
					@gamepaused(false)
					@gameended(false)
					$("#ghostbusters-song").get(0).pause()
					$("#ghostbusters-theme").get(0).currentTime = 0
					$("#ghostbusters-theme").get(0).play()
				when 2 # game paused
					@gameready(false)
					@gamestarted(false)
					@gamepaused(true)
					@gameended(false)
				when 3 # game ended
					@gameready(false)
					@gamestarted(false)
					@gamepaused(false)
					@gameended(true)
					$("#ghostbusters-song").get(0).currentTime = 0
					$("#ghostbusters-song").get(0).play()
					$("#ghostbusters-theme").get(0).pause()
		
		generateUID: ->
			id = ""
			id += Math.random().toString(36).substr(2) while id.length < 8
			id.substr 0, 8
		
		setGameName: (game_name) ->
			@gamename(game_name)
			game_details = game_name.split "__"
			@gamecreator(game_details[0].split("_").join(" "))
			date = new Date(parseInt( game_details[1], 10 ));
			hours = date.getHours()
			minutes = "0" + date.getMinutes()
			seconds = "0" + date.getSeconds()
			@gametime(hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2))
		
		refreshPlayerList: (json) ->
			# Compute missing players
			@gamemaxplayers(json.game.n_players)
			playersmissing = json.game.n_players - json.game.players.length
			@gameplayersmissing(playersmissing)
			@game_team_RED.removeAll()
			@game_team_BLUE.removeAll()
			if json.game.players.length > 0
				i = 0
				for player in json.game.players
					i = i + 1
					player.index = i
					if (player.team == 0)
						@game_team_RED.push(player)
					else if (player.team == 1)
						@game_team_BLUE.push(player)
		
		showMessage: (msg_code, option) ->
			c = msg_code
			switch c
				when -1
					# NO_TRAP - no enough money to set a trap
					message = "You cannot set a trap, asshole!"
					type = "alert"
				when -2
					# OUT_OF_AREA - out of area
					message = "Come inside, quick, you'll catch a cold out there!"
					type = "show-banner"
				when -3
					# T_NEEDS_KEY - the treasure needs the key to be opened
					message = "This treasure is locked and you don't have the right key."
					type = "alert"
					$("#treasure-locked").get(0).play()
				when -4
					# NOT_ENOUGH_PLAYERS - not enough player
					message = "Some moron leaved the mission and has not come back in time..."
					type = "alert"
				when -5
					# T_EMPTY - treasure empty
					message = "oh oh, there's nothing here."
					type = "alert"
				when -6
					# NO_T_NEAR_YOU - no treasure nearby
					message = "There are no treasure near you, moron."
					type = "alert"
				when 1
					# PARANORMAL_ATTACK - attacked from ghost
					message = "Ghost Attack! You lost " + option + "$!"
					type = "alert"
					$("#ghost-attack").get(0).play()
				when 2
					# HUMAN_ATTACK - attacked from human
					message = "Ouch!"
					type = "alert"
					$("#player-attack").get(0).play()
				when 3
					# KEY_FOUND - key found
					message = "You have found a key! Yay!"
					type = "alert"
					$("#keys-found").get(0).play()
				when 4
					# GOLD_FOUND - gold found
					message = "You have found " + option + "$! You are filthy rich now!"
					type = "alert"
					$("#gold-found").get(0).play()
				when 5
					# K_G_FOUND - key and gold found
					message = "Jackpot! " + option + "$ and a key!"
					type = "alert"
					$("#gold-found").get(0).play()
					$("#keys-found").delay(600).get(0).play()
				when 7
					# BACK_IN_AREA - player has returned inside the game area
					message = "Your team won this game! Congratulations!"
					type = "hide-banner"
			
			alert = $('#message-alert')
			alert.html(message)
			if (c < 0)
				color = "red"
			else if (c > 0)
				color = "blue"
			
			alert.css('background-color', color);
			if (type == "alert")
				alert.fadeIn('fast').delay(1000).fadeOut('fast').delay(300)
			else if (type == "show-banner")
				alert.fadeIn('fast')
			else if (type == "hide-banner")
				alert.fadeOut('fast')
				
			console.log(message)
					
			
	return MainPageModel

