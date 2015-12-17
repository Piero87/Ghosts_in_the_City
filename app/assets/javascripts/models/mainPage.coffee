#
# The main page.
#
# This class handles most of the user interactions with the buttons/menus/forms on the page, as well as manages
# the WebSocket connection.	It delegates to other classes to manage everything else.
#
define ["knockout", "gps"], (ko, Gps) ->
	class MainPageModel
		constructor: () ->
		
			# User name
			@username = ko.observable()
			
			# Game data
			@gameid = ko.observable()
			@gamename = ko.observable()
			
			# Interval to send a lot of request for available games
			@interval = null
			
			# Games list
			@gameslist = ko.observableArray()
			
			# The Web Socket
			@ws = null
			
			@disconnected = ko.observable(true)
			@connected = ko.observable(false)
			
			@connecting = ko.observable()
			@closing = false
			
			@game = ko.observable(false)
			
			# Load previously user name if set
			if localStorage.username
				@username(localStorage.username)
				@connect()
		
		# Connect
		connect: ->
			username = @username()
			@connecting("Connecting...")
			@disconnected(null)
			
			# Open Web Socket
			@ws = new WebSocket(jsRoutes.controllers.Application.stream(username).webSocketURL())
			
			# When the websocket opens
			@ws.onopen = (event) =>
				@connecting(null)
				@connected(true)
				@interval = setInterval(gamesList, 500)
				
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
					localStorage.removeItem("username")
			
			# Handle the stream
			@ws.onmessage = (event) =>
				json = JSON.parse(event.data)
				console.log(JSON.stringify(json))	
				if json.event = "user-positions"
					console.log('User Position Received!')
					# Update all the markers on the map
					#@map.updateMarkers(json.positions.features)
				else if json.event in ["new_game", "game_joined"]
					clearInterval(@interval) if(@interval)
					@game(true)
					@gameid(json.game.id)
					localStorage.setItem("gameid", @gameid())
					@gamename(json.game.name)
				else if json.event = "games_list"
					console.log('games_list')
					@gameslist.removeAll()
					for game in json.games_list
						@gameslist.push(game)
		
		# The user clicked connect
		submitUsername: ->
			localStorage.setItem("username", @username())
			@connect()
		
		# New Game 
		newGame: ->
			gamename = @gamename()
			console.log("New Game")
			@ws.send(JSON.stringify
				event: "new_game"
				game: 
					name: gamename
			)
		
		# Games list
		gamesList: ->
			username = @username()
			console.log("Games List")
			@ws.send(JSON.stringify
				event: "games_list"
				list: []
			)
		
		# Join Game 
		joinGame: (gameid) ->
			alert(gameid)
			console.log("Join Game")
			@ws.send(JSON.stringify
				event: "join_game"
				game: 
					id: gameid
			)
		
		# Disconnect the ws
		disconnect: ->
			@closing = true
			@ws.close()
							
	return MainPageModel

