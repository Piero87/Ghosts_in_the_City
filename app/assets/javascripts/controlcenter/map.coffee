#
# The main map.  Manages displaying markers on the map, as well as responding to the user moving around and zooming
# on the map.
#
define ["marker", "leaflet"], (Marker, Leaflet) ->

	class Map
		constructor: (ws) ->
			# the map itself
			@map = Leaflet.map("mapContainer")
			new Leaflet.TileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
				minZoom: 17
				maxZoom: 20
				attribution: "Map data © OpenStreetMap contributors"
			).addTo(@map)
			
			# The websocket
			@ws = ws
			
			# Ghosts icons
			# LEVEL 1
			@g1 = L.icon(
				iconUrl: '/assets/images/Ghost_L1_left.png'
				iconSize: [
					32
					32
				])
				
			@g1_angry = L.icon(
				iconUrl: '/assets/images/Ghost_L1_Angry_left.png'
				iconSize: [
					32
					32
				])
				
			@g1_scared = L.icon(
				iconUrl: '/assets/images/Ghost_L1_Scared_left.png'
				iconSize: [
					32
					32
				])
				
			# LEVEL 2
			@g2 = L.icon(
				iconUrl: '/assets/images/Ghost_L2_left.png'
				iconSize: [
					32
					32
				])
			@g2_angry = L.icon(
				iconUrl: '/assets/images/Ghost_L2_Angry_left.png'
				iconSize: [
					32
					32
				])
				
			@g2_scared = L.icon(
				iconUrl: '/assets/images/Ghost_L2_Scared_left.png'
				iconSize: [
					32
					32
				])
			
			# LEVEL 2
			@g3 = L.icon(
				iconUrl: '/assets/images/Ghost_L3_left.png'
				iconSize: [
					32
					32
				])
			@g3_angry = L.icon(
				iconUrl: '/assets/images/Ghost_L3_Angry_left.png'
				iconSize: [
					32
					32
				])
				
			@g3_scared = L.icon(
				iconUrl: '/assets/images/Ghost_L3_Scared_left.png'
				iconSize: [
					32
					32
				])
			
			# Treasure icons
			@t_opened = L.icon(
				iconUrl: '/assets/images/Treasure_open.png'
				iconSize: [
					32
					32
				])
			
			@t_closed = L.icon(
				iconUrl: '/assets/images/Treasure_close.png'
				iconSize: [
					32
					32
				])
				
			# Trap icons
			@trap_active = L.icon(
				iconUrl: '/assets/images/Trap_active.png'
				iconSize: [
					32
					32
				])
			
			@trap_idle = L.icon(
				iconUrl: '/assets/images/Trap_unactive.png'
				iconSize: [
					32
					32
				])
			
			@map_keys = 
				37 : false # Left
				38 : false # Up
				39 : false # Right
				40 : false # Down
				65 : false # A
				83 : false # S
				68 : false # D
			
		# Init Map with one player position and right zoom 
		initMap: (lat, lon) ->
			@map.setView([lat, lon], 18)
			
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
			@addBuster(
				buster.uid, buster.name, buster.team, buster.pos.latitude, buster.pos.longitude
			) for buster in players
			
		addBusterMarker: (uid, name, team, lat, lng) ->
			type = "buster"
			# Buster icon
			b_icon = L.icon(
				iconUrl: '/assets/images/G' + (@b_markers.length % 4) + '.png'
				iconSize: [
					32
					32
				]
				popupAnchor: [
					0
					-15
				])
			marker = new Marker(@map,type, uid, name, team, lat , lng)
			marker.setIcon(b_icon)
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
					g_icon = @g1()
				else if (mood == 1)
					g_icon = @g1_angry()
				else 
					g_icon = @g1_scared()
			else if(level == 2)
				if(mood == 0)
					g_icon = @g2()
				else if (mood == 1)
					g_icon = @g2_angry()
				else 
					g_icon = @g2_scared()
			else
				if(mood == 0)
					g_icon = @g3()
				else if (mood == 1)
					g_icon = @g3_angry()
				else 
					g_icon = @g3_scared()
			marker = new Marker(@map,type, uid, name, team, lat, lng)
			marker.setIcon(g_icon)
			@g_markers.push marker
		
		setTreasureMarkers: (treasures) ->
			# The treasures markers on the map
			@t_markers = []
			@addTreasureMarker(
				treasure.uid, treasure.status, treasure.pos.latitude, treasure.pos.longitude
			) for treasure in treasures
			
		addTreasure: (uid, status, lat, lng) ->
			type = "treasure"
			name = ""
			team = ""
			t_icon = ""
			if(status == 0)
			# Treasure closed
				t_icon = @t_closed()
			else
				t_icon = @t_open()
			marker = new Marker(@map,type, uid, name, team, lat , lng)
			marker.setIcon(t_icon)
			@t_markers.push marker
		
		setTrapsMarker: (traps)  ->
			# The treasures markers on the map
			@traps_markers = []
			@addTrapMarker(
				trap.uid, trap.status, trap.pos.latitude, trap.pos.longitude
			) for trap in traps
			
		setTrapMarker: (uid, lat, lng)  ->
			# The treasures markers on the map
			@addTrapMarker(uid, 0, lat, lng)
			
		addTreasure: (uid, status, lat, lng) ->
			type = "trap"
			name = ""
			team = ""
			trap_icon = ""
			if(status == 0)
			# Trap idle
				trap_icon = @trap_idle()
			else
				trap_icon = @trap_active()
			marker = new Marker(@map,type, uid, name, team, lat , lng)
			marker.setIcon(trap_icon)
			@traps_markers.push marker
		
		updateActiveTrapMarker: (uid) ->
			for marker, i in @traps_markers when marker.uid == uid
				marker.setIcon(@trap_active())
		
		removeTrapMarker: (uid) ->
			for marker, i in @traps_markers when marker.uid == uid
				marker.remove()
		
		updateBusterMarkers: (uid, lat, lng) ->
			for marker, i in @b_markers when marker.uid == uid
				marker.update(lat, lng)
		
		updateGhostMarkers: (uid, level, mood, lat, lon) ->
			for marker, i in @g_markers when marker.uid == uid
				g_icon = "" 
				if(level == 1)
					if(mood == 0)
						g_icon = @g1()
					else if (mood == 1)
						g_icon = @g1_angry()
					else 
						g_icon = @g1_scared()
				else if(level == 2)
					if(mood == 0)
						g_icon = @g2()
					else if (mood == 1)
						g_icon = @g2_angry()
					else 
						g_icon = @g2_scared()
				else
					if(mood == 0)
						g_icon = @g3()
					else if (mood == 1)
						g_icon = @g3_angry()
					else 
						g_icon = @g3_scared()
				marker.update(lat, lng)
				marker.setIcon(g_icon)
		
		updateTreasureMarkers: (uid, status) ->
			for marker, i in @t_markers when marker.uid == uid
				t_icon = ""
				if(status == 0)
				# Treasure closed
					t_icon = @t_closed()
				else
					t_icon = @t_open()
				marker.setIcon(t_icon)
				
	return Map