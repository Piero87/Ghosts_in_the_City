#
# A marker class
#
define ["leaflet"], (Leaflet) ->

	class Marker
		constructor: (map, type, uid, name, team, level, lat , lng, icon) ->
			@clicked = false
			
			@map = map
			@uid = uid
			@name = name
			@team = team
			@level = level
			@lat = lat
			@lng = lng
			@markericon = icon
			
			latlng = new Leaflet.LatLng(lat, lng)
			
			# Custom buster marker popup based on team	
			@customOptionsRed =
				'className': 'custom-red'
			
			@customOptionsBlue =
				'className': 'custom-blue'
			
			t = type
			switch t
				when "buster" # Buster
					#if (@team == "red")
						@marker = new Leaflet.Marker(latlng, {icon: @markericon}).bindPopup(@name, @customOptionsRed).addTo(@map)
					#else
						@marker = new Leaflet.Marker(latlng, {icon: @markericon}).bindPopup(@name, @customOptionsBlue).addTo(@map)
				
				when "ghost" # Ghost
					# Check the level and the mood of the ghost
					@marker = new Leaflet.Marker(latlng)
					if level == 3 
						@marker.on 'click', @onClick()
					@marker.addTo(@map)
				
				when "treasure" # Treasure
					@marker = new Leaflet.Marker(latlng)
					@marker.addTo(@map)
				
				when "trap" # Trap
					@marker = new Leaflet.Marker(latlng)
					@marker.addTo(@map)
					
		# Update buster marker position with the given latLng coordinates
		update: (lat, lng) ->
			# Update the position
			latlng = new Leaflet.LatLng(lat, lng)
			@marker.setLatLng(latlng)
			
		#Set icon
		setMarkerIcon: (icon) ->
			@marker.setIcon(icon)
	
		# Remove the marker from the map
		remove: () ->
			@map.removeLayer(@marker)
			
		# onClick function. It activate the ghost manual mode for admin
		onClick: () ->
			if !@clicked()
				@map.updateGhostMarkers(@uid, 3, 1, @lat, @lng)
				@map.ghostManualMode(uid) 
			else
				@map.updateGhostMarkers(@uid, 3, 0, @lat, @lng)
				@map.ghostNormalMode(uid)
			
	return Marker