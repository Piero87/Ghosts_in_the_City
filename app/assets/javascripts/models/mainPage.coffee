#
# The main page.
#
# This class handles most of the user interactions with the buttons/menus/forms on the page, as well as manages
# the WebSocket connection.  It delegates to other classes to manage everything else.
#
define ["knockout", "gps", "mockGps"], (ko, Gps, MockGps) ->

  class MainPageModel
    constructor: () ->
      # the current user
      @email = ko.observable()

      # Contains a message to say that we're either connecting or reconnecting
      @connecting = ko.observable()
      @disconnected = ko.observable(true)

      # The MockGps model
      @mockGps = ko.observable()
      # The GPS model
      @gps = ko.observable()

      # If we're closing
      @closing = false

      # Load the previously entered email if set
      if localStorage.email
        @email(localStorage.email)
        @connect()

    # The user clicked connect
    submitEmail: ->
      localStorage.email = @email()
      @connect()

    # Connect function. Connects to the websocket, and sets up callbacks.
    connect: ->
      email = @email()
      @connecting("Connecting...")
      @disconnected(null)

      @ws = new WebSocket(jsRoutes.controllers.Application.stream(email).webSocketURL())

      # When the websocket opens, create a new map and new GPS
      @ws.onopen = (event) =>
        @connecting(null)
        @gps(new Gps(@ws))

      @ws.onclose = (event) =>
        # Need to handle reconnects in case of errors
        if (!event.wasClean && !self.closing)
          @connect()
          @connecting("Reconnecting...")
        else
          @disconnected(true)
        @closing = false
        # Destroy everything and clean it all up.
        @mockGps().destroy() if @mockGps()
        @gps().destroy() if @gps()
        @mockGps(null)
        @gps(null)

      # Handle the stream of feature updates
      @ws.onmessage = (event) =>
        json = JSON.parse(event.data)
 #       if json.event == "user-positions"
          # Update all the markers on the map
 #         @map.updateMarkers(json.positions.features)

    # Disconnect the web socket
    disconnect: ->
      @closing = true
      @ws.close()

  return MainPageModel

