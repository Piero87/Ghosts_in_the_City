define () ->
	class GameMap
		constructor: (id_user, players, id_canvas, canvas_width, canvas_height, websocket) -> #(id_user, players, ghosts, id_canvas, canvas_width, canvas_height, websocket)
			
			@ws = websocket
			@user_id = id_user
			# Y position of buster
			@canvas = document.getElementById(id_canvas)
			# canvas
			@ctx = undefined
			# context
			@emptyBack = new Image
			
			@busters = []
			@busters_images = []
			
			@addBuster(buster.uid, buster.name, buster.x, buster.y) for buster in players
			
			@ghosts = []
			@ghosts_images = []
			
			#@addGhost(ghost.id, ghost.x, ghost.y) for ghost in ghosts
			
			@space_width = canvas_width
			@space_height = canvas_height
			@icon_dim = 32
			@move = 5
			
		startGame: ->
			console.log("Start Game!")
			# Make sure you got the context.
			if @canvas.getContext
				# If you have it, create a canvas user interface element.
				# Specify 2d canvas type.
				@ctx = @canvas.getContext('2d')
				# Paint it black.
				@ctx.fillStyle = 'grey'
				@ctx.rect 0, 0, @space_width, @space_height
				@ctx.fill()
				# Save the initial background.
				@emptyBack = @ctx.getImageData(0, 0, @icon_dim, @icon_dim)
			# Play the game until the until the game is over.
			callback_interval = @doGameLoop.bind(this)
			gameLoop = setInterval(callback_interval, 16)
			# Add keyboard listener.
			callback_key = @whatKey.bind(this)
			window.addEventListener 'keydown', @whatKey.bind(this), true
			return
		
		addBuster: (uid, name, x, y) ->
			buster = {}
			buster.uid = uid
			buster.name = name
			buster.x = x
			# current buster position X
			buster.y = y
			# current buster position Y
			buster.old_x = x
			# old buster position X
			buster.old_y = y
			# old buster position Y
			@busters.push buster
			buster_img = new Image
			# buster
			buster_img.src = '/assets/images/G' + @busters.length + '.png'
			@busters_images.push buster_img
			return
		
		busterMove: (uid, x, y) ->
			for buster, i in @busters when buster.uid == uid
				@busters[i].old_x = buster.x
				@busters[i].old_y = buster.y
				@busters[i].x = x
				@busters[i].y = y
			
		addGhost: (uid, x, y) ->
			ghost = {}
			ghost.uid = uid
			ghost.x = x
			# current ghost position X
			ghost.y = y
			# current ghost position Y
			ghost.old_x = x
			# old ghost position X
			ghost.old_y = y
			# old ghost position Y
			@ghosts.push ghost
			ghost_img = new Image
			# ghost
			ghost_img.src = '/assets/images/ghost_right.png'
			@ghosts_images.push ghost_img
			return
		
		ghostMove: (uid, x, y) ->
			for ghost, i in @ghosts when ghost.uid == uid
				@ghosts[i].old_x = ghost.x
				@ghosts[i].old_y = ghost.y
				@ghosts[i].x = x
				@ghosts[i].y = y
		
		doGameLoop: ->
			
			for buster, i in @busters
				@ctx.putImageData(@emptyBack, @busters[i].old_x, @busters[i].old_y);
				@ctx.drawImage(@busters_images[i], @busters[i].x, @busters[i].y, @icon_dim, @icon_dim);
			
			for ghost, i in @ghosts
				@ctx.putImageData(@emptyBack, @ghosts[i].old_x, @ghosts[i].old_y);
				@ctx.drawImage(@ghosts_images[i], @ghosts[i].x, @ghosts[i].y, @icon_dim, @icon_dim);
		
			return
		
		# Get key press.
		
		whatKey: (evt) ->
			for buster, i in @busters when buster.uid == @user_id
				
				# Arrows keys
				arrows = [
					37
					38
					39
					40
				]
				# Flag to put variables back if we hit an edge of the board.
				flag = 0
				# Get where the buster was before key process.
				@busters[i].old_x = @busters[i].x
				@busters[i].old_y = @busters[i].y
				if arrows.indexOf(evt.keyCode) == -1
					return false
				switch evt.keyCode
					# Left arrow.
					when 37
						@busters[i].x = @busters[i].x - @move
						if @busters[i].x < 0
							# If at edge, reset buster position and set flag.
							@busters[i].x = 0
							flag = 1
					# Right arrow.
					when 39
						@busters[i].x = @busters[i].x + @move
						if @busters[i].x > @space_width - @icon_dim
							# If at edge, reset buster position and set flag.
							@busters[i].x = @space_width - @move
							flag = 1
					# Down arrow
					when 40
						@busters[i].y = @busters[i].y + @move
						if @busters[i].y > @space_height - @icon_dim
							# If at edge, reset buster position and set flag.
							@busters[i].y = @space_height - @move
							flag = 1
					# Up arrow 
					when 38
						@busters[i].y = @busters[i].y - @move
						if @busters[i].y < 0
							# If at edge, reset buster position and set flag.
							@busters[i].y = 0
							flag = 1
				
				@ws.send(JSON.stringify
					event: "update_position"
					user: @user_id
					x: @busters[i].x
					y: @busters[i].y
				)
				
				# If flag is set, the buster did not move.
				# Put everything backBuster the way it was.
				if flag
					@busters[i].x = @busters[i].old_x
					@busters[i].y = @busters[i].old_y
	        	
	return GameMap