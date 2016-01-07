#
# The main page.
#
# This class handles most of the user interactions with the buttons/menus/forms on the page, as well as manages
# the WebSocket connection.	It delegates to other classes to manage everything else.
#
define ["knockout", "gps", "gameMap"], (ko, Gps, GameMap) ->
	class MainPageModel
		constructor: () ->
		
			# User data
			@username = ko.observable()
			@useruid = ko.observable()
			@user = {uid: "", name: "", team: "", x: 0, y: 0}
			
			# Game data
			@gameready = ko.observable(false)
			@gamestarted = ko.observable(false)
			@gamepaused = ko.observable(false)
			@gameended  = ko.observable(false)
			@gameid = ko.observable()
			@gamename = ko.observable()
			@gamemaxplayers = ko.observable(2)
			@gameplayersmissing = ko.observable()
			@gameplayers = ko.observableArray()
			
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
				
				@user.name = localStorage.username
				@user.uid = localStorage.uid
				
				@connect()
				
				if localStorage.gameid
					@gameid(localStorage.gameid)
					@resumeGame()
		
		# Connect
		connect: ->
			@connecting("Connecting...")
			@disconnected(null)
			
			@ws = new WebSocket(jsRoutes.controllers.Application.login(@user.name, @user.uid).webSocketURL())
			
			# When the websocket opens
			@ws.onopen = (event) =>
				@connecting(null)
				@connected(true)
				
				# Setting the interval for refresh games list
				callback = @gamesList.bind(this)
				@interval = setInterval(callback, 1000)
				
			# When the websocket closes
			@ws.onclose = (event) =>
				# Handle the reconnection in case of errors
				if (!event.wasClean && ! self.closing)
					@connect()
					@connecting("Reconnecting ...")
				else
					# Destroy everything and clean all
					@disconnected(true)
					@connected(false)
					@closing = false
					localStorage.removeItem("uid")
					localStorage.removeItem("username")
			
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
					clearInterval(@interval) if(@interval)
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
							@refreshPlayerList(json)
						when 1 # game started
							console.log(JSON.stringify(json))
							@refreshPlayerList(json)
							@game_map = new GameMap(@user.uid, json.game.players, json.game.ghosts, json.game.treasures, @ws)
							@game_map.startGame()
						when 2 # game paused
							console.log('Hold on!')
						when 3 # game ended
							console.log('Game Over!')
							localStorage.removeItem("gameid")			
				else if json.event == "update_player_position"
					@game_map.busterMove(json.user.uid, json.user.pos.x, json.user.pos.y)
				else if json.event == "update_ghosts_positions"
					@game_map.ghostMove(ghost.uid, ghost.mood, ghost.pos.x, ghost.pos.y) for ghost in json.ghosts
				else if json.event == "update_treasures"
					@game_map.updateTreasure(treasure.uid, treasure.status) for treasure in json.treasures
							
		# The user clicked connect
		submitUsername: ->
			@user.uid = @generateUID()
			@useruid(@user.uid)
			@user.name = @username()
			localStorage.setItem("uid", @user.uid)
			localStorage.setItem("username", @user.name)
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
			#console.log("Games List")
			@ws.send(JSON.stringify
				event: "games_list"
			)
		
		# Join Game 
		joinGame: (game) ->
			console.log("Join Game")
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
			clearInterval(@interval) if(@interval)
			@changeGameStatus(-1)
			@closing = true
			@ws.close()
		
		# Leave Game
		leaveGame: ->
			@changeGameStatus(-1)
			@gamename("")
			@gamemaxplayers(2)
			@gameplayers.removeAll()
			
			callback = @gamesList.bind(this)
			@interval = setInterval(callback, 1000)
			
			@ws.send(JSON.stringify
				event: "leave_game"
			)
		
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
  			@gameplayers.removeAll()
  			if json.game.players.length > 0
  				for player in json.game.players
  					console.log("giocatore in attesa:" + player.name)
  					@gameplayers.push(player)
  					
	return MainPageModel

