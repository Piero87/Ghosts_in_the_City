#
# A marker class
#
define ["leaflet"], (Leaflet) ->

	class Marker
		constructor: (map_view, type, uid, name, team, level, lat , lng, icon, angry_icon, ws) ->
			
			
			@clicked = false
			
			@map = map_view
			@uid = uid
			@name = name
			@team = team
			@level = level
			@lat = lat
			@lng = lng
			@markericon = icon
			@angryicon = angry_icon
			@ws = ws
			
			latlng = new Leaflet.LatLng(lat, lng)
			
			# Custom buster marker popup based on team	
			@customOptionsRed =
				'className': 'custom-red'
			
			@customOptionsBlue =
				'className': 'custom-blue'
			
			t = type
			switch t
				when "buster" # Buster
					if @team == "red"
						@marker = new Leaflet.Marker(latlng, {icon: @markericon}).bindPopup(@name, @customOptionsRed).addTo(@map)
					else if @team == "blue"
						@marker = new Leaflet.Marker(latlng, {icon: @markericon}).bindPopup(@name, @customOptionsBlue).addTo(@map)
				
				when "ghost" # Ghost
					# Check the level and the mood of the ghost
					@marker = new Leaflet.Marker(latlng, {icon: @markericon})
					if level == 3 
						@marker.on 'click', @onClick
					@marker.addTo(@map)
					@circle = new Leaflet.Circle(latlng, 3*level, {color: 'white', fillColor: '#fff', fillOpacity: 0.5}).addTo(@map)
				
				when "treasure" # Treasure
					@marker = new Leaflet.Marker(latlng, {icon: @markericon}).addTo(@map)
					@circle = new Leaflet.Circle(latlng, 6, {color: 'yellow', fillColor: '#f2ce00', fillOpacity: 0.5}).addTo(@map)
				
				when "trap" # Trap
					@marker = new Leaflet.Marker(latlng, {icon: @markericon}).addTo(@map)
					@circle = new Leaflet.Circle(latlng, 2, {color: 'grey', fillColor: '#7f8f9f', fillOpacity: 0.5}).addTo(@map)
					
		# Update buster marker position with the given latLng coordinates
		update: (lat, lng) ->
			# Update the position
			latlng = new Leaflet.LatLng(lat, lng)
			@marker.setLatLng(latlng)
			@circle.setLatLng(latlng)
			
		#Set icon
		setMarkerIcon: (icon) ->
			@marker.setIcon(icon)
	
		# Remove the marker from the map
		remove: () ->
			@map.removeLayer(@marker)
			@map.removeLayer(@circle)
			
		# onClick function. It activate the ghost manual mode for admin
		onClick: (evt) ->
			if(localStorage.ghost_possessed == "")
				if (@clicked != true)
					@clicked = true
					@setIcon(@angryicon)
					localStorage.setItem("ghost_possessed", @uid)
				
					# Tell the server to restart the ghost
					@ws.send(JSON.stringify
						event: "ghost_manual_mode"
						ghost_uid: @uid
					)
				else
					@clicked = false
					localStorage.setItem("ghost_possessed", "")
					
					# Release the ghost
					@ws.send(JSON.stringify
						event: "ghost_normal_mode"
						ghost_uid: @uid
					)
			else
				console.log("A ghost is already possessed")
			
	return Marker