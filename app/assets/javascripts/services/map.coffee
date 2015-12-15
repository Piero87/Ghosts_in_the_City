#
# The visualization of the game.  Shows the status of the game: the positions of players and ghosts
#

class Map
	constructor: () ->
		@canvas = undefined
		@ctx = undefined
		@emptyBack = new Image
		@gameLoop = undefined
		@gameFrame = 0
		@busters = []
		@busters_images = []
		@ghosts = []
		@ghosts_images = []
		@space_width = 500
		@space_heigth = 500
		@icon_dim = 32
		@move = 5
		return
		
	canvasSpaceGame = ->
		# Get the canvas element.
		canvas = document.getElementById('gameArena')
		# Make sure you got it.
		if canvas.getContext
			# Specify 2d canvas type.
			ctx = canvas.getContext('2d')
			# Paint it black.
			ctx.fillStyle = 'grey'
			ctx.rect 0, 0, space_width, space_heigth
			ctx.fill()
			# Save the initial background.
			emptyBack = ctx.getImageData(0, 0, icon_dim, icon_dim)
		# Play the game until the until the game is over.
		gameLoop = setInterval(doGameLoop, 16)
		# Add keyboard listener.
		window.addEventListener 'keydown', whatKey, true
		return
	
	addBuster = (id, x, y) ->
		buster = {}
		buster.index = i + 1
		buster.x = 0
		# current buster position X
		buster.y = 0
		# current buster position Y
		buster.old_x = x
		# old buster position X
		buster.old_y = y
		# old buster position Y
		@busters[id] = buster
		buster_img = new Image
		# buster
		buster_img.src = 'G' + busters.length + '_right.png'
		@busters_images[id] buster_img
		return
	
	updateBusterPosition = (id, x, y) ->
		@busters[id].x = x
		@busters[id].y = y
		return
	
	addGhost = (id, x, y) ->
		ghost = {}
		ghost.index = i + 1
		ghost.img = new Image
		# ghost
		ghost.img.src = 'Ghost_right.png'
		ghost.x = Math.floor(Math.random() * 450) + 50
		# current ghost position X
		ghost.y = Math.floor(Math.random() * 450) + 50
		# current ghost position Y
		ghost.old_x = 0
		# old buster position X
		ghost.old_y = 0
		# old buster position Y
		@ghosts[id] = ghost
		ghost_img = new Image
		# buster
		ghost_img.src = 'Ghost_right.png'
		@ghosts_images[id] = ghost_img

	updateGhostPosition = (id, x, y) ->
		@ghosts[id].x = x
		@ghosts[id].y = y
		return
	
	makeGhosts = (n) ->
		i = 0
		while i < n
			ghost = {}
			ghost.index = i + 1
			ghost.img = new Image
			# ghost
			ghost.img.src = 'Ghost_right.png'
			ghost.x = Math.floor(Math.random() * 450) + 50
			# current ghost position X
			ghost.y = Math.floor(Math.random() * 450) + 50
			# current ghost position Y
			ghost.old_x = 0
			# old buster position X
			ghost.old_y = 0
			# old buster position Y
			ghosts.push ghost
			ghost_img = new Image
			# buster
			ghost_img.src = 'Ghost_right.png'
			ghosts_images.push ghost_img
			i++
		return
	
	doGameLoop = ->
		gameFrame++
		i = 0
		while i < busters.length
			ctx.putImageData emptyBack, busters[i].old_x, busters[i].old_y
			ctx.drawImage busters_images[i], busters[i].x, busters[i].y
			i++
			if gameFrame == 10
				gameFrame = 0
				index = 0
				while index < ghosts.length
					ghostMove index
					ctx.putImageData emptyBack, ghosts[index].old_x, ghosts[index].old_y
					ctx.drawImage ghosts_images[index], ghosts[index].x, ghosts[index].y
					index++
		return

