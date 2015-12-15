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
				if json.event == "user-positions"
					console.log('user-positions')
					# Update all the markers on the map
								#@map.updateMarkers(json.positions.features)
				else if json.event == "new-game"
								@game(true)
		
		# The user clicked connect
		submitUsername: ->
			localStorage.setItem("username", @username())
			@connect()
		
		# New Game 
		newGame: ->
			console.log("New Game")
			@ws.send(JSON.stringify
				event: "new-game"
				name: "culo"
			)
		
		# Disconnect the ws
		disconnect: ->
			@closing = true
			@ws.close()
							
	return MainPageModel

