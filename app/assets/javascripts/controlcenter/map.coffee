#
# The main map.  Manages displaying markers on the map, as well as responding to the user moving around and zooming
# on the map.
#
define ["marker", "leaflet"], (Marker, Leaflet) ->

	class Map
		constructor: (ws) ->
			
			# the map itself
			@map = Leaflet.map("mapContainer")
			new Leaflet.TileLayer("https://api.tiles.mapbox.com/v4/mapbox.streets/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoibWFyemFjazg3IiwiYSI6ImNpbTNoaXFwODAwcHB1eG00cXN5dWNobWUifQ.VeMWhSTA2gzKz0jkCnRgFg",
				minZoom: 1
				maxZoom: 20
				attribution: attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
			).addTo(@map)
			
			@map.setView([0,0], 2)
			
			# The websocket
			@ws = ws
			
			@ghost_possessed = ""
			
			@earth_rad = 6371010
			@move = 1
			
			# Ghosts icons
			# LEVEL 1
			@g1 = Leaflet.icon(
				iconUrl: '/assets/images/Ghost_L1_left.png'
				iconSize: [
					32
					32
				])
				
			@g1_angry = Leaflet.icon(
				iconUrl: '/assets/images/Ghost_L1_Angry_left.png'
				iconSize: [
					32
					32
				])
				
			@g1_scared = Leaflet.icon(
				iconUrl: '/assets/images/Ghost_L1_Scared_left.png'
				iconSize: [
					32
					32
				])
				
			# LEVEL 2
			@g2 = Leaflet.icon(
				iconUrl: '/assets/images/Ghost_L2_left.png'
				iconSize: [
					32
					32
				])
			@g2_angry = Leaflet.icon(
				iconUrl: '/assets/images/Ghost_L2_Angry_left.png'
				iconSize: [
					32
					32
				])
				
			@g2_scared = Leaflet.icon(
				iconUrl: '/assets/images/Ghost_L2_Scared_left.png'
				iconSize: [
					32
					32
				])
			
			# LEVEL 2
			@g3 = Leaflet.icon(
				iconUrl: '/assets/images/Ghost_L3_left.png'
				iconSize: [
					32
					32
				])
			@g3_angry = Leaflet.icon(
				iconUrl: '/assets/images/Ghost_L3_Angry_left.png'
				iconSize: [
					32
					32
				])
				
			@g3_scared = Leaflet.icon(
				iconUrl: '/assets/images/Ghost_L3_Scared_left.png'
				iconSize: [
					32
					32
				])
			
			# Treasure icons
			@t_opened = Leaflet.icon(
				iconUrl: '/assets/images/Treasure_open.png'
				iconSize: [
					32
					32
				])
			
			@t_closed = Leaflet.icon(
				iconUrl: '/assets/images/Treasure_close.png'
				iconSize: [
					32
					32
				])
				
			# Trap icons
			@trap_active = Leaflet.icon(
				iconUrl: '/assets/images/Trap_active.png'
				iconSize: [
					32
					32
				])
			
			@trap_idle = Leaflet.icon(
				iconUrl: '/assets/images/Trap_unactive.png'
				iconSize: [
					32
					32
				])
			
			# Listeners	
			@callback_keydown = @keyPressed.bind(this)
			@callback_keyup = @keyReleased.bind(this)
			@ghost_manual_mode = false
			
			@map_keys = 
				37 : false # Left
				38 : false # Up
				39 : false # Right
				40 : false # Down
				68 : false # D
			
		# Init Map with one player position and right zoom 
		initMap: (lat, lng, zoom) ->
			latlng = new Leaflet.LatLng(lat, lng)
			@map.setView(latlng, zoom)
			
		# Start Game	
		startGame: () ->
			# Add keyboard listener
			window.addEventListener 'keydown', @callback_keydown, true
			window.addEventListener 'keyup', @callback_keyup, true
				
		# Pause Game
		pauseGame: () ->
			# Remove keyboard listener.
			window.removeEventListener 'keydown', @callback_keydown, true
			window.removeEventListener 'keyup', @callback_keyup, true
			
			@destroy()
			
		# End Game	
		endGame: () ->
			@destroy()
			
		# Destroy the map
		destroy: ->
			try
				@map.remove()
			catch e
		
		setBusterMarkers: (players) ->
			# The busters markers on the map
			@b_markers = []
			@addBusterMarker(
				buster.uid, buster.name, buster.team, buster.pos.latitude, buster.pos.longitude
			) for buster in players
			
		addBusterMarker: (uid, name, team, lat, lng) ->
			type = "buster"
			level = ""
			buster_team = ""
			# Buster icon
			b_icon = Leaflet.icon(
				iconUrl: '/assets/images/G1.png'
				iconSize: [
					32
					32
				]
				popupAnchor: [
					0
					-15
				])
			if(team == 0)
				buster_team = "red"
			else
				buster_team = "blue"
			marker = new Marker(@map,type, uid, name, buster_team, level, lat , lng, b_icon, this, this)
			@b_markers.push marker
		
		setGhostMarkers: (ghosts) ->
			# The ghosts markers on the map
			@g_markers = []
			@addGhostMarker(
				ghost.uid, ghost.level, ghost.mood, ghost.pos.latitude, ghost.pos.longitude
			) for ghost in ghosts
			
		addGhostMarker: (uid, level, mood, lat, lng) ->
			type = "ghost"
			team = ""
			name = ""
			g_icon = "" 
			if(level == 1)
				if(mood == 0)
					g_icon = @g1
				else if (mood == 1)
					g_icon = @g1_angry
				else 
					g_icon = @g1_scared
			else if(level == 2)
				if(mood == 0)
					g_icon = @g2
				else if (mood == 1)
					g_icon = @g2_angry
				else 
					g_icon = @g2_scared
			else
				if(mood == 0)
					g_icon = @g3
				else if (mood == 1)
					g_icon = @g3_angry
				else 
					g_icon = @g3_scared
			marker = new Marker(@map, type, uid, name, team, level, lat, lng, g_icon, this)
			@g_markers.push marker
		
		setTreasuresMarkers: (treasures) ->
			# The treasures markers on the map
			@t_markers = []
			@addTreasureMarker(
				treasure.uid, treasure.status, treasure.pos.latitude, treasure.pos.longitude
			) for treasure in treasures
			
		addTreasureMarker: (uid, status, lat, lng) ->
			type = "treasure"
			name = ""
			team = ""
			level = ""
			t_icon = ""
			if(status == 0)
			# Treasure closed
				t_icon = @t_closed
			else
				t_icon = @t_open
			marker = new Marker(@map,type, uid, name, team, level, lat , lng, t_icon, this)
			@t_markers.push marker
		
		setTrapsMarkers: (traps)  ->
			# The treasures markers on the map
			@traps_markers = []
			@addTrapMarker(
				trap.uid, trap.status, trap.pos.latitude, trap.pos.longitude
			) for trap in traps
			
		setTrapMarker: (uid, lat, lng)  ->
			# The treasures markers on the map
			@addTrapMarker(uid, 0, lat, lng)
			
		addTrapMarker: (uid, status, lat, lng) ->
			type = "trap"
			name = ""
			team = ""
			level = ""
			trap_icon = ""
			if(status == 0)
			# Trap idle
				trap_icon = @trap_idle()
			else
				trap_icon = @trap_active()
			marker = new Marker(@map,type, uid, name, team, level lat , lng, trap_icon, this)
			@traps_markers.push marker
		
		updateActiveTrapMarker: (uid) ->
			for marker, i in @traps_markers when marker.uid == uid
				marker.setMarkerIcon(@trap_active)
		
		removeTrapMarker: (uid) ->
			for marker, i in @traps_markers when marker.uid == uid
				marker.remove()
		
		updateBusterMarkers: (uid, lat, lng) ->
			for marker, i in @b_markers when marker.uid == uid
				marker.update(lat, lng)
		
		updateGhostMarkers: (uid, level, mood, lat, lng) ->
			for marker, i in @g_markers when marker.uid == uid
				g_icon = "" 
				if(level == 1)
					if(mood == 0)
						g_icon = @g1
					else if (mood == 1)
						g_icon = @g1_angry
					else 
						g_icon = @g1_scared
				else if(level == 2)
					if(mood == 0)
						g_icon = @g2
					else if (mood == 1)
						g_icon = @g2_angry
					else 
						g_icon = @g2_scared
				else
					if(mood == 0)
						g_icon = @g3
					else if (mood == 1)
						g_icon = @g3_angry
					else 
						g_icon = @g3_scared
				marker.update(lat, lng)
				marker.setMarkerIcon(g_icon)
		
		updateTreasureMarkers: (uid, status) ->
			for marker, i in @t_markers when marker.uid == uid
				t_icon = ""
				if(status == 0)
				# Treasure closed
					t_icon = @t_closed
				else
					t_icon = @t_open
				marker.setMarkerIcon(t_icon)
		
		# Get key press
		keyPressed: (evt) ->
			if evt.keyCode of @map_keys
				@map_keys[evt.keyCode] = true 
				evt.preventDefault()
			@action()
			
		# Get key released	
		keyReleased: (evt) ->
			if evt.keyCode of @map_keys
				@map_keys[evt.keyCode] = false 
				evt.preventDefault()
			@action()
			
		ghostNormalMode: (uid) ->
			# Tell the server to restart the ghost
			@ghost_possessed = ""
			@ws.send(JSON.stringify
				event: "ghost_normal_mode"
				ghost_uid: @ghost_possessed
			)
				
		ghostManualMode: (uid) ->
			if (@ghost_possessed == "")	
				@ghost_possessed = uid
				
				@ws.send(JSON.stringify
					event: "ghost_manual_mode"
					ghost_uid: @ghost_possessed
				)
			
		action: ->
			for marker, i in @g_markers when marker.uid == @ghost_possessed
				
				if @map_keys[68]
					@ws.send(JSON.stringify
						event: "ghost_hit_player"
						ghost_uid: @ghost_possessed
					)
				
				if @map_keys[37] || @map_keys[38] || @map_keys[39] || @map_keys[40]
					angle = 0
					if @map_keys[38] && @map_keys[37]  # up-left
						angle = 3 * Math.PI / 4
					else if @map_keys[40] && @map_keys[37]  # down-left
						angle = - 3 * Math.PI / 4
					else if @map_keys[38] && @map_keys[39]  # up-right
						angle = Math.PI / 4
					else if @map_keys[40] && @map_keys[39]  # down-right
						angle = - Math.PI / 4
					else if @map_keys[38] # up
						angle = Math.PI / 2
					else if @map_keys[40] # down
						angle = - Math.PI / 2
					else if @map_keys[37] # left
						angle = Math.PI
					else if @map_keys[39] # right
						angle = 0
					
					latitude = marker[i].getLatLng().latitude
					longitude = marker[i].getLatLng().longitude
					
					lat_rad = latitude * Math.PI / 180
					lng_rad = longitude * Math.PI / 180
					
					delta_lat = (@move * Math.cos( angle ) / @earth_rad) * 180 / Math.PI
					delta_lng = ((@move * Math.cos( angle ) / @earth_rad) / Math.cos(lat_rad)) * 180 / Math.PI
					
					new_lat = latitude + delta_lat
					new_lng = longitude + delta_lng
					
					marker[i].setLatLng([new_lat, new_lng])
					
					@ws.send(JSON.stringify
						event: "update_posghost_position"
						ghost_uid: @ghost_possessed
						pos:
							latitude: new_lat
							longitude: new_lng
					)
					
	return Map