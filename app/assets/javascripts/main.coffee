#
# The main entry point into the client side. Creates a new main page model and binds it to the page.
#
require.config {
  paths: {
    mainPage: "./models/mainPage"
    gameClientEngine: "./services/gameClientEngine"
    md5: "./md5.min"
    bootstrap: "../lib/bootstrap/js/bootstrap"
    jquery: "../lib/jquery/jquery"
    knockout: "../lib/knockout/knockout"
    map: "./controlcenter/map"
    marker: "./controlcenter/marker"
    leaflet: "../lib/leaflet/leaflet"
  }
  shim: {
    bootstrap: {
      deps: ["jquery"],
      exports: "$"
    }
    jquery: {
      exports: "$"
    }
    knockout: {
      exports: "ko"
    }
    leaflet: {
      exports: "L"
    }
  }
}

require ["knockout", "mainPage", "bootstrap"], (ko, MainPageModel) ->

  model = new MainPageModel
  ko.applyBindings(model)

