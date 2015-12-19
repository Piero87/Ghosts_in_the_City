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
			@uuid
			@username = ko.observable()
			
			# Game data
			@gameready = ko.observable(false)
			@gamestarted = ko.observable(false)
			@gamepaused = ko.observable(false)
			@gameended  = ko.observable(false)
			@gameid = ko.observable()
			@gamename = ko.observable()
			@gamemaxplayers = ko.observable(0)
			@gamewaitingforplayers = ko.observable(0)
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
				@uuid = localStorage.uuid
				@connect()
		
		# Connect
		connect: ->
			username = @username()
			@connecting("Connecting...")
			@disconnected(null)
			
			# Open Web Socket
			@ws = new WebSocket(jsRoutes.controllers.Application.stream(username, @uuid).webSocketURL())
			
			# When the websocket opens
			@ws.onopen = (event) =>
				@connecting(null)
				@connected(true)
				
				# Setting the interval for refresha games list
				callback = @gamesList.bind(this)
				@interval = setInterval(callback, 500)
				
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
				console.log(JSON.stringify(json))	
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
				else if json.event == "game_status"
					# {event: "game_status", game: {id: [Int], name: [String], n_players: [Int], players [Array of String], status: [Int]}}
					switch json.game.status
						when 0 # game waiting
							console.log('Wait!')
							
							# Set system status variables
							@gameready(true)
							@gamestarted(false)
							@gamepaused(false)
							@gameended(false)
							
							# Compute missing players
							@gamemaxplayers(json.game.n_players)
							@gamewaitingforplayers(json.game.n_players - json.game.players.length)
							@gameplayers.removeAll()
							if json.game.players.length > 0
								for player in json.game.players
									@gameplayers.push(player)
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
			@uuid = generateUUID
			localStorage.setItem("username", @username())
			localStorage.setItem("uuid", @uuid)
			@connect()
		
		# New Game 
		newGame: ->
			username = @username()
			gamename = @gamename()
			gamemaxplayers = @gamemaxplayers()
			console.log("New Game")
			@ws.send(JSON.stringify
				event: "new_game"
				source: username
				user_name: username
				uuid: @uuid
				name: gamename
				n_players: parseInt( gamemaxplayers, 10 )
			)
		
		# Games list
		gamesList: ->
			username = @username()
			console.log("Games List")
			@ws.send(JSON.stringify
				event: "games_list"
				source: username
				list: []
			)
		
		# Join Game 
		joinGame: (gameid) ->
			username = @username()
			console.log("Join Game")
			@ws.send(JSON.stringify
				event: "join_game"
				source: username
				game: 
					id: gameid
			)
		
		# Disconnect the ws
		disconnect: ->
			clearInterval(@interval) if(@interval)
			@closing = true
			@ws.close()
		
		generateUUID = ->
			d = (new Date).getTime()
			uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) ->
				r = (d + Math.random() * 16) % 16 | 0
				d = Math.floor(d / 16)
				(if c == 'x' then r else r & 0x3 | 0x8).toString 16
			)
			uuid
							
	return MainPageModel

