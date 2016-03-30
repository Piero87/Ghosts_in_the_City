#
# The main map.  Manages displaying markers on the map, as well as responding to the user moving around and zooming
# on the map.
#
define ["marker", "leaflet"], (Marker, Leaflet) ->

	class Map
		constructor: () ->
			
			# the map itself
			@map = Leaflet.map("mapContainer")
			new Leaflet.TileLayer("https://api.tiles.mapbox.com/v4/mapbox.streets/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoibWFyemFjazg3IiwiYSI6ImNpbTNoaXFwODAwcHB1eG00cXN5dWNobWUifQ.VeMWhSTA2gzKz0jkCnRgFg",
				minZoom: 1
				maxZoom: 20
				attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
			).addTo(@map)
			
			@map.setView([0,0], 2)
			
			@earth_rad = 6371010
			
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
			
		# Init Map with one player position and right zoom 
		initMap: (lat, lng, zoom) ->
			latlng = new Leaflet.LatLng(lat, lng)
			@map.setView(latlng, zoom)
			
		addGameArea: (point1, point2, point3, point4) ->
			@game_area = Leaflet.polygon([
				[
					point1.latitude
					point1.longitude
				]
				[
					point2.latitude
					point2.longitude
				]
				[
					point3.latitude
					point3.longitude
				]
				[
					point4.latitude
					point4.longitude
				]
			]).addTo(@map)
			
		# Pause Game
		pauseGame: () ->
			@destroy()
			
		# End Game	
		endGame: () ->
			@destroy()
			
		# Destroy the map
		destroy: ->
			try
				for marker, i in @b_markers 
					marker[i].remove()
				for marker, i in @g_markers 
					marker[i].remove()
				for marker, i in @t_markers 
					marker[i].remove()
				for marker, i in @traps_markers 
					marker[i].remove()
				@map.remove()
			catch e
			map_container = document.getElementById("mapContainer")
			while map_container.firstChild
				map_container.removeChild(map_container.firstChild)
		
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
				iconUrl: '/assets/images/G' + ((@b_markers.length % 4)+1) + '.png'
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
			marker = new Marker(@map,type, uid, name, buster_team, level, lat , lng, b_icon,"")
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
			g_angry_icon = "" 
			if(level == 1)
				if(mood == 0)
					g_icon = @g1
					g_angry_icon = @g1_angry
				else if (mood == 1)
					g_icon = @g1_angry
					g_angry_icon = @g1
				else 
					g_icon = @g1_scared
					g_angry_icon = @g1
			else if(level == 2)
				if(mood == 0)
					g_icon = @g2
					g_angry_icon = @g2_angry
				else if (mood == 1)
					g_icon = @g2_angry
					g_angry_icon = @g2
				else 
					g_icon = @g2_scared
					g_angry_icon = @g2
			else
				if(mood == 0)
					g_icon = @g3
					g_angry_icon = @g3_angry
				else if (mood == 1)
					g_icon = @g3_angry
					g_angry_icon = @g3
				else 
					g_icon = @g3_scared
					g_angry_icon = @g3
			marker = new Marker(@map, type, uid, name, team, level, lat, lng, g_icon, g_angry_icon)
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
			marker = new Marker(@map,type, uid, name, team, level, lat , lng, t_icon,"")
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
				trap_icon = @trap_idle
			else
				trap_icon = @trap_active
			marker = new Marker(@map,type, uid, name, team, level, lat , lng, trap_icon, "")
			@traps_markers.push marker
		
		updateActiveTrapMarker: (uid) ->
			for marker, i in @traps_markers when marker.uid == uid
				marker.setMarkerIcon(@trap_active)
		
		removeTrapMarker: (uid) ->
			trap_index = -1
			for marker, i in @traps_markers when marker.uid == uid
				marker.remove()
				trap_index = i
			@traps_markers.splice(trap_index, 1)
		
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
		
	return Map