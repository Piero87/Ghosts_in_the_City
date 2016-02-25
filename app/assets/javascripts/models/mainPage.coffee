#
# The main page.
#
# This class handles most of the user interactions with the buttons/menus/forms on the page, as well as manages
# the WebSocket connection.	It delegates to other classes to manage everything else.
#
define ["knockout", "gps", "gameClientEngine"], (ko, Gps, GameClientEngine) ->
	class MainPageModel
		constructor: () ->
		
			# User data
			@username = ko.observable()
			@useruid = ko.observable()
			@usergold = ko.observable()
			@userkeys = ko.observable()
			
			@usergold(0)
			@userkeys(0)
			
			# Game data
			@gameready = ko.observable(false)
			@gamestarted = ko.observable(false)
			@gamepaused = ko.observable(false)
			@gameended= ko.observable(false)
			@gameid = ko.observable()
			@gamename = ko.observable()
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
			
			# Load previously user name if set
			if localStorage.username
				@username(localStorage.username)
				@useruid(localStorage.uid)
				
				@connect()
		
		# Connect
		connect: ->
			@connecting("Connecting...")
			@disconnected(null)
			
			@ws = new WebSocket(jsRoutes.controllers.Application.login(@username(), @useruid()).webSocketURL())
			
			# When the websocket opens
			@ws.onopen = (event) =>
				@connecting(null)
				@connected(true)
				
				@game_client_engine = new GameClientEngine(@useruid(), @ws)
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
					localStorage.removeItem("username")
					localStorage.removeItem("gameid")
			
			# Handle the stream
			@ws.onmessage = (event) =>
				json = JSON.parse(event.data)
				if json.event == "user-positions"
					console.log('User Position Received!')
					# Update all the markers on the map
					#@map.updateMarkers(json.positions.features)
					
				else if json.event == "games_list"
					#console.log('Games list received!')
					@gameslist.removeAll()
					if json.list.length > 0
						@gamesavailable(true)
						for game in json.list
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
					@gamename(json.game.name)
					@gamemaxplayers(json.game.n_players)
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
							console.log(json)
							console.log('Fight!')
							@refreshPlayerList(json)
							@gamename(json.game.name)
							@game_client_engine.setBusters(json.game.players)
							@game_client_engine.setGhosts(json.game.ghosts)
							@game_client_engine.setTreasures(json.game.treasures)
							@game_client_engine.startGame()
							@usergold(player.gold) for player in json.game.players when player.uid == @useruid()
						when 2 # game paused
							console.log('Hold on!')
							@game_client_engine.pauseGame()
						when 3 # game ended
							@game_client_engine = null
							console.log 'Game Over!'
							@gamename("")
							localStorage.removeItem("gameid")
										
				else if json.event == "update_player_position"
					if @gamestarted()
						@game_client_engine.busterMove(json.user.uid, json.user.pos.x, json.user.pos.y)
						
				else if json.event == "update_user_info"
					if @gamestarted()
						console.log(json)
						@usergold(json.user.gold)
						@userkeys(json.user.keys.length)
						
				else if json.event == "update_ghosts_positions"
					if @gamestarted()
						@game_client_engine.ghostMove(ghost.uid, ghost.mood, ghost.pos.x, ghost.pos.y) for ghost in json.ghosts
				
				else if json.event == "update_treasures"
					console.log("Tesoro aperto!")
					console.log(json.treasures)
					if @gamestarted()
						@game_client_engine.changeTreasureStatus(treasure.uid, treasure.status) for treasure in json.treasures
				
				else if json.event == "new_trap"
					if @gamestarted()
						@game_client_engine.newTrap(json.trap.uid, json.trap.pos.x, json.trap.pos.y)
						console.log "Nuova trappola!"
						# console.log json.trap
				
				else if json.event == "active_trap"
					if @gamestarted()
						@game_client_engine.activeTrap(json.trap.uid) if (json.trap.status == 1)
						console.log "Trappola attivata!"
						# console.log json.trap
				
				else if json.event == "remove_trap"
					if @gamestarted()
						@game_client_engine.removeTrap(json.trap.uid)
						console.log "Trappola rimossa!"
						# console.log json.trap
				
				else if json.event == "message"
					@showMessage(json.code)
					
		# The user clicked connect
		submitUsername: ->
			@useruid(@generateUID())
			localStorage.setItem("uid", @useruid())
			localStorage.setItem("username", @username())
			@connect()
		
		# New Game 
		newGame: ->
			gamename = @gamename()
			gamemaxplayers = @gamemaxplayers()
			console.log("New Game")
			@ws.send(JSON.stringify
				event: "new_game"
				name: gamename
				n_players: parseInt( gamemaxplayers, 10 )
			)
		
		# Games list
		gamesList: ->
			@ws.send(JSON.stringify
				event: "games_list"
			)
		
		# Join Game 
		joinGame: (game) ->
			@ws.send(JSON.stringify
				event: "join_game"
				game: game
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
		
		generateUID: ->
			id = ""
			id += Math.random().toString(36).substr(2) while id.length < 8
			id.substr 0, 8
		
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
		
		showMessage: (msg_code) ->
			c = msg_code
			switch c
				when -1
					# no enough money to set a trap
					message = "You cannot set a trap, asshole!"
					type = "error"
				when -2
					# out of area
					message = "Come inside, quick, you'll catch a cold out there!"
					type = "error"
				when -3
					# the treasure needs the key to be opened
					message = "This treasure is locked and you don't have the right key."
					type = "error"
				when -4
					# not enough player
					message = "Some moron is got out from the game and has not come back in time..."
					type = "error"
				when -5
					# treasure empty
					message = "Oh oh, there's nothing here."
					type = "error"
				when -6
					# no treasure nearby
					message = "There are no treasure near you, moron."
					type = "error"
				when 1
					# attacked from ghost
					message = "Aaaaaaaah!"
					type = "message"
				when 2
					# attacked from human
					message = "Ouch!"
					type = "message"
				when 3
					# key found
					message = "You have found a key! Yay!"
					type = "message"
				when 4
					# gold found
					message = "You have found some gold! You are filthy rich now!"
					type = "message"
				when 5
					# key and gold found
					message = "Jackpot! You have found a key and some gold!"
					type = "message"
				when 6
					# victory
					message = "Your team won this game! Congratulations!"
					type = "message"
				when 7
					# lost
					message = "Your team has been defeated! Looooooosers!"
					type = "message"
			
			alert = $('#message-alert')
			alert.html(message)
			if (type == "error")
				color = "red"
			else if (type == "message")
				color = "blue"
			
			alert.css('background-color', color);
			alert.fadeIn('fast').delay(1000).fadeOut('fast').delay(300)
			
			console.log(message)
					
			
	return MainPageModel

