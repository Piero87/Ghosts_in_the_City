#
# A marker class
#
define ["leaflet"], (Leaflet) ->

	class Marker
		constructor: (map, type, uid, name, team, lat , lng) ->
			@map = map
			@uid = uid
			@name = name
			@team = team
			
			# Custom buster marker popup based on team	
			@customOptionsRed =
				'className': 'custom-red'
			
			@customOptionsBlue =
				'className': 'custom-blue'
			
			t = type
			switch t
				when "buster" # Buster
					@marker = new Leaflet.Marker([lat, lng])
					if (@team == "red")
						@marker.bindPopup(@name(), @customOptionsRed())
					else
						@marker.bindPopup(@name(), @customOptionsBlue())
					@marker.addTo(map)
				
				when "ghost" # Ghost
					# Check the level and the mood of the ghost
					@marker = new Leaflet.Marker([lat, lng])
					@marker.addTo(map)
				
				when "treasure" # Treasure
					@marker = new Leaflet.Marker([lat, lng])
					@marker.addTo(map)
				
				when "trap" # Trap
					@marker = new Leaflet.Marker([lat, lng])
					@marker.addTo(map)
					
		# Update buster marker position with the given latLng coordinates
		update: (lat, lng) ->
			# Update the position
			@marker.setLatLng([lat, lng])
	
		# Remove the marker from the map
		remove: () ->
			@map.removeLayer(@marker)
			
	return Marker