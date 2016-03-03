define () ->
	class GameClientEngine
		constructor: (id_player, websocket) ->

			@space_width = $("#conf_space_width").val()
			@space_height = $("#conf_space_height").val()
			@icon_size = $("#conf_icon_size").val()
			@move = 5
			@ghost_radius = $("#conf_ghost_radius").val()
			@treasure_radius = $("#conf_treasure_radius").val()
			@trap_radius = $("#conf_trap_radius").val()
			
			@debug = false;
			
			@gameLoop = null

			@ws = websocket
			@player_id = id_player

			# canvas
			@ctx = undefined
			# context
			@emptyBack = new Image

			@traps = []

			@callback_key = @whatKey.bind(this)

			@sensible_area = new Image
			@sensible_area.src = '/assets/images/Area.png'

			# Colori squadre

			@team_red = new Image
			@team_red.src = '/assets/images/Team_red.png'

			@team_blue = new Image
			@team_blue.src = '/assets/images/Team_blue.png'

			@team_red_you = new Image
			@team_red_you.src = '/assets/images/YOU_Team_red.png'

			@team_blue_you = new Image
			@team_blue_you.src = '/assets/images/YOU_Team_blue.png'

			# Treasure

			@treasure_open = new Image
			@treasure_open.src = '/assets/images/Treasure_open.png'
			
			@treasure_close = new Image
			@treasure_close.src = '/assets/images/Treasure_close.png'
			
			# Trap
			
			@trap_active = new Image
			@trap_active.src = '/assets/images/Trap_active.png'
			
			@trap_unactive = new Image
			@trap_unactive.src = '/assets/images/Trap_unactive.png'
			
		initCanvas: ->
			@resetCanvas()
			canvas_container = document.getElementById("gameArenaContainer")
			@canvas = document.createElement("canvas")
			@canvas.width = @space_width
			@canvas.height = @space_height
			canvas_container.appendChild(@canvas)
			# Make sure you got the context.
			if @canvas.getContext
				# If you have it, create a canvas player interface element.
				# Specify 2d canvas type.
				@ctx = @canvas.getContext('2d')
				# Paint it black.
				@ctx.fillStyle = 'grey'
				@ctx.rect 0, 0, @space_width, @space_height
				@ctx.fill()
				# Save the initial background.
				@emptyBack = @ctx.getImageData(0, 0, @space_width, @space_height)
		
		resetCanvas: ->
			canvas_container = document.getElementById("gameArenaContainer")
			while canvas_container.firstChild
				canvas_container.removeChild(canvas_container.firstChild)
		
		startGame: ->
			console.log "GAME MAP - Start Game!"
			@initCanvas()
			# Play the game until the until the game is over.
			callback_interval = @doGameLoop.bind(this)
			@gameLoop = setInterval(callback_interval, 60)
			# Add keyboard listener.
			window.addEventListener 'keydown', @callback_key, true
		
		pauseGame: ->
			console.log "GAME MAP - Pause Game!"
			if (@gameLoop)
				clearInterval(@gameLoop)
				# Add keyboard listener.
				window.removeEventListener 'keydown', @callback_key, true
				@resetCanvas()
		
		endGame: ->
			@resetCanvas()
			
		setBusters: (players) ->
			@busters = []
			@busters_images = []
			@addBuster(
				buster.uid, buster.name, buster.team, buster.pos.latitude, buster.pos.longitude
			) for buster in players
		
		addBuster: (uid, name, team, latitude, longitude) ->
			buster = {}
			buster.uid = uid
			buster.name = name
			buster.team = team
			buster.latitude = latitude
			# current buster latitude position
			buster.longitude = longitude
			# current buster longitude position
			buster.old_latitude = latitude
			# old buster latitude position
			buster.old_longitude = longitude
			# old buster longitude position
			@busters.push buster
			buster_img = new Image
			# buster
			buster_img.src = '/assets/images/G' + @busters.length + '.png'
			@busters_images.push buster_img
		
		busterMove: (uid, latitude, longitude) ->
			for buster, i in @busters when buster.uid == uid
				@busters[i].old_latitude = buster.latitude
				@busters[i].old_longitude = buster.longitude
				@busters[i].latitude = latitude
				@busters[i].longitude = longitude
		
		setGhosts: (ghosts) ->
			@ghosts = []
			@ghosts_images = []
			@addGhost(
				ghost.uid, ghost.level, ghost.mood, ghost.pos.latitude, ghost.pos.longitude
			) for ghost in ghosts
			
		addGhost: (uid, level, mood, latitude, longitude) ->
			ghost = {}
			ghost.uid = uid
			ghost.level = level
			ghost.mood = mood
			ghost.latitude = latitude
			# current ghost position X
			ghost.longitude = longitude
			# current ghost position Y
			ghost.old_latitude = latitude
			# old ghost position X
			ghost.old_longitude = longitude
			# old ghost position Y
			@ghosts.push ghost
			ghost_img = new Image
			# ghost
			ghost_img.src = '/assets/images/Ghost_L' + level + '_right.png'
			@ghosts_images.push ghost_img
		
		ghostMove: (uid, mood, latitude, longitude) ->
			for ghost, i in @ghosts when ghost.uid == uid
				@ghosts[i].mood = mood
				if @ghosts[i].mood == 0
					if ghost.latitude > x
						@ghosts_images[i].src = '/assets/images/Ghost_L' + @ghosts[i].level + '_left.png'
					else
						@ghosts_images[i].src = '/assets/images/Ghost_L' + @ghosts[i].level + '_right.png'
				else if @ghosts[i].mood == 1 # Oh oh, someone is angry...
					if ghost.latitude > x
						@ghosts_images[i].src = '/assets/images/Ghost_L' + @ghosts[i].level + '_Angry_left.png'
					else
						@ghosts_images[i].src = '/assets/images/Ghost_L' + @ghosts[i].level + '_Angry_right.png'
				else if @ghosts[i].mood == 2 # Poor trapped ghost
					console.log "Fantasma in trappola"
					console.log @ghosts[i]
					if ghost.latitude > x
						@ghosts_images[i].src = '/assets/images/Ghost_L' + @ghosts[i].level + '_Scared_left.png'
					else
						@ghosts_images[i].src = '/assets/images/Ghost_L' + @ghosts[i].level + '_Scared_right.png'
				@ghosts[i].old_latitude = ghost.latitude
				@ghosts[i].old_longitude = ghost.longitude
				@ghosts[i].latitude = latitude
				@ghosts[i].longitude = longitude
		
		setTreasures: (treasures) ->
			@treasures = []
			
			@addTreasure(
				treasure.uid, treasure.status, treasure.pos.latitude, treasure.pos.longitude
			) for treasure in treasures
		
		addTreasure: (uid, status, latitude, longitude) ->
			treasure = {}
			treasure.uid = uid
			treasure.name = name
			treasure.status = status
			treasure.latitude = latitude
			# current buster position X
			treasure.longitude = longitude
			# current buster position Y
			@treasures.push treasure
		
		doGameLoop: ->
			
			@ctx.putImageData(@emptyBack, 0, 0);
			
			for treasure, i in @treasures
				# To center the images in their position point
				treasure_latitude = @treasures[i].latitude - (@icon_size / 2)
				treasure_longitude = @treasures[i].longitude - (@icon_size / 2)
				area_latitude =  @treasures[i].latitude -  @treasure_radius
				area_longitude =  @treasures[i].longitude -  @treasure_radius
				
				if (@debug)
					# Drawings
					@ctx.drawImage(
						@sensible_area
						area_latitude 
						area_longitude 
						(@treasure_radius * 2)
						(@treasure_radius * 2)
					)
				
				if (@treasures[i].status == 0)
					treasure_img = @treasure_close
				else if (@treasures[i].status == 1)
					treasure_img = @treasure_open
				
				@ctx.drawImage(
					treasure_img
					treasure_latitude
					treasure_longitude
					@icon_size
					@icon_size
				) 
				
			for buster, i in @busters
				# To center the image in its position point
				buster_latitude = @busters[i].latitude - (@icon_size / 2)
				buster_longitude = @busters[i].longitude - (@icon_size / 2)
				# Drawings
				@ctx.drawImage(
					@busters_images[i]
					buster_latitude
					buster_longitude
					@icon_size
					@icon_size
				)
				# Draw team square color in the corner bottom-right
				
				if buster.uid == @player_id
					if buster.team == 0
						team_img = @team_red_you
					else if buster.team == 1
						team_img = @team_blue_you
				else
					if buster.team == 0
						team_img = @team_red
					else if buster.team == 1
						team_img = @team_blue
				
				@ctx.drawImage(
						team_img
						(buster_latitude + (@icon_size - 8))
						(buster_longitude + (@icon_size - 8))
						8
						8
					)
				
			for ghost, i in @ghosts
				# To center the images in their position point
				ghost_latitude = @ghosts[i].latitude - (@icon_size / 2)
				ghost_longitude = @ghosts[i].longitude - (@icon_size / 2)
				area_latitude =  @ghosts[i].latitude - (@ghost_radius * @ghosts[i].level)
				area_longitude =  @ghosts[i].longitude - (@ghost_radius * @ghosts[i].level)
				
				if (@debug)
					# Drawings
					@ctx.drawImage(
						@sensible_area
						area_latitude
						area_longitude
						(@ghost_radius * 2 * @ghosts[i].level)
						(@ghost_radius * 2 * @ghosts[i].level)
					)
				
				@ctx.drawImage(
					@ghosts_images[i]
					ghost_latitude
					ghost_longitude
					@icon_size
					@icon_size
				)
				
			for trap, i in @traps
				# To center the images in their position point
				trap_latitude = @traps[i].latitude - (@icon_size / 2)
				trap_longitude = @traps[i].longitude - (@icon_size / 2)
				area_latitude =  @traps[i].latitude -  @trap_radius
				area_longitude =  @traps[i].longitude -  @trap_radius
				
				if (@debug)
					# Drawings
					@ctx.drawImage(
						@sensible_area
						area_latitude
						area_longitude
						(@trap_radius * 2)
						(@trap_radius * 2)
					)
				
				if (@traps[i].status == 0)
					trap_img = @trap_unactive
				else if (@traps[i].status == 1)
					trap_img = @trap_active

				@ctx.drawImage(
					trap_img
					trap_latitude
					trap_longitude
					@icon_size
					@icon_size
				)
		
		# Get key press.
		
		whatKey: (evt) ->
			for buster, i in @busters when buster.uid == @player_id
				
				# Arrows keys
				keys = [
					37
					38
					39
					40
					65
					83
					68
				]
				# 65 = "a" => set a trap
				# 83 = "s" => open treasure
				# 68 = "d" => hit other player
				
				if keys.indexOf(evt.keyCode) == -1
					return false
				evt.preventDefault();
				switch evt.keyCode
					# "a" key
					when 65
						@ws.send(JSON.stringify
							event: "set_trap"
						)
					# "s" key
					when 83
						@ws.send(JSON.stringify
							event: "open_treasure"
						)
					# "d" key
					when 68
						@ws.send(JSON.stringify
							event: "hit_player"
						)
					when 37, 38, 39, 40
						@movement(evt.keyCode, i)
		
		newTrap: (uid, latitude, longitude) ->
			trap = {}
			trap.uid = uid
			trap.status = 0 # unactive
			trap.latitude = latitude
			# current buster position X
			trap.longitude = longitude
			@traps.push trap
		
		activeTrap: (trap_uid) ->
			for trap, i in @traps when trap.uid == trap_uid
				trap.status = 1
				
		removeTrap: (trap_uid) ->
			trap_index = -1
			for trap, i in @traps when trap.uid == trap_uid
				trap_index = i
			@traps.splice(trap_index, 1) 
			
		changeTreasureStatus: (uid, status) ->
			for treasure, i in @treasures when treasure.uid == uid
				@treasures[i].status = status
				if (status == 1 && @treasures[i].status == 0) 
					$("#treasure-opening").get(0).play()
		
		movement: (direction, i) ->
			# Flag to put variables back if we hit an edge of the board.
			flag = 0
			# Get where the buster was before key process.
			@busters[i].old_latitude = @busters[i].latitude
			@busters[i].old_longitude = @busters[i].longitude
			switch direction
				# Left arrow.
				when 37
					@busters[i].latitude = @busters[i].latitude - @move
					if @busters[i].latitude < (@icon_size / 2)
						# If at edge, reset buster position and set flag.
						@busters[i].latitude = (@icon_size / 2)
						flag = 1
				# Right arrow.
				when 39
					@busters[i].latitude = @busters[i].latitude + @move
					if @busters[i].latitude > @space_width - (@icon_size / 2)
						# If at edge, reset buster position and set flag.
						@busters[i].latitude = @space_width - @move
						flag = 1
				# Down arrow
				when 40
					@busters[i].longitude = @busters[i].longitude + @move
					if @busters[i].longitude > @space_height - (@icon_size / 2)
						# If at edge, reset buster position and set flag.
						@busters[i].longitude = @space_height - @move
						flag = 1
				# Up arrow 
				when 38
					@busters[i].longitude = @busters[i].longitude - @move
					if @busters[i].longitude < (@icon_size / 2)
						# If at edge, reset buster position and set flag.
						@busters[i].longitude = (@icon_size / 2)
						flag = 1
			
			# If flag is set, the buster did not move.
			# Put everything backBuster the way it was.
			if flag
				@busters[i].latitude = @busters[i].old_latitude
				@busters[i].longitude = @busters[i].old_longitude
			
			@ws.send(JSON.stringify
				event: "update_player_position"
				pos:
					latitude: @busters[i].latitude
					longitude: @busters[i].longitude
			)
		
		generateUID: ->
  			id = ""
  			id += Math.random().toString(36).substr(2) while id.length < 8
  			id.substr 0, 8
			
		toggleDebug: (debug) ->
			if (debug)
				@debug = true
			else
				@debug = false
			
	return GameClientEngine