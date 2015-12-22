#
# The main page.
#
# This class handles most of the user interactions with the buttons/menus/forms on the page, as well as manages
# the WebSocket connection.	It delegates to other classes to manage everything else.
#
define ["knockout", "gps"], (ko, Gps) ->
	class MainPageModel
		constructor: () ->
		
			# User data
			@username = ko.observable()
			@user = {uid: "", name: "", team: ""}
			
			# Game data
			@gameready = ko.observable(false)
			@gamestarted = ko.observable(false)
			@gamepaused = ko.observable(false)
			@gameended  = ko.observable(false)
			@gameid = ko.observable()
			@gamename = ko.observable()
			@gamemaxplayers = ko.observable()
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
				@user.name = localStorage.username
				@user.uid = localStorage.uuid
				@connect()
		
		# Connect
		connect: ->
			@connecting("Connecting...")
			@disconnected(null)
			
			# Open Web Socket

			@ws = new WebSocket(jsRoutes.controllers.Application.stream(@user.name, @user.id).webSocketURL())
			
			# When the websocket opens
			@ws.onopen = (event) =>
				@connecting(null)
				@connected(true)
				
				# Setting the interval for refresha games list
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
					localStorage.removeItem("uuid")
					localStorage.removeItem("username")
			
			# Handle the stream
			@ws.onmessage = (event) =>
				json = JSON.parse(event.data)
				if json.event == "user-positions"
					console.log('User Position Received!')
					# Update all the markers on the map
					#@map.updateMarkers(json.positions.features)
				else if json.event == "games_list"
					console.log('Games list received!')
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
					switch json.game.status
						when 0 # game waiting
							console.log('Wait!')
							console.log(json.game.players)
							
							# Set system status variables
							@gameready(true)
							@gamestarted(false)
							@gamepaused(false)
							@gameended(false)
							
							@refreshPlayerList(json)
							
						when 1 # game started
							console.log('Fight!')
							
							# Set system status variables
							@gameready(false)
							@gamestarted(true)
							@gamepaused(false)
							@gameended(false)
							
							@gameplayers.removeAll()
							if json.game.players.length > 0
								for player in json.game.players
									@gameplayers.push(player)
							
						when 2 # game paused
							console.log('Hold on!')
							@gameready(false)
							@gamestarted(false)
							@gamepaused(true)
							@gameended(false)
						when 3 # game ended
							console.log('Game Over!')
							
							localStorage.removeItem("gameid")
							
							@gameready(false)
							@gamestarted(false)
							@gamepaused(false)
							@gameended(true)
		
		# The user clicked connect
		submitUsername: ->
			@user.uid = generateUID()
			@user.name = @username()
			localStorage.setItem("uuid", @user.uid)
			localStorage.setItem("username", @user.name)
			@connect()
		
		# New Game 
		newGame: ->
			gamename = @gamename()
			gamemaxplayers = @gamemaxplayers()
			console.log("New Game")
			@ws.send(JSON.stringify
				event: "new_game"
				user: @user
				name: gamename
				n_players: parseInt( gamemaxplayers, 10 )
			)
		
		# Games list
		gamesList: ->
			console.log("Games List")
			@ws.send(JSON.stringify
				event: "games_list"
				user: @user
				list: []
			)
		
		# Join Game 
		joinGame: (game) ->
			console.log("Join Game")
			@ws.send(JSON.stringify
				event: "join_game"
				user: @user
				game: game
			)
		
		# Disconnect the ws
		disconnect: ->
			clearInterval(@interval) if(@interval)
			@closing = true
			@ws.close()
		
		# Leave Game
		leaveGame: ->
			@gameready(false)
			@gamestarted(false)
			@gamepaused(false)
			@gameended(false)
			@ws.send(JSON.stringify
				event: "leave_game"
				user: @user
				game_id: @gameid
			)
		
		generateUID = ->
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

