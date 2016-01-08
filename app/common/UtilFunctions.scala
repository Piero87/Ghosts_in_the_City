package common

import scala.util.Random
import com.typesafe.config.ConfigFactory
import play.api.Logger

class CustomLogger (o: String) {
  var origin = o
  def log(msg: String) {
    var milliseconds = System.currentTimeMillis()
    var seconds = (milliseconds / 1000) % 60;
    var minutes = ((milliseconds / (1000*60)) % 60);
    var hours = ((milliseconds / (1000*60*60)) % 24);
    Logger.info("[" + hours + ":" + minutes + ":" + seconds + " - " + origin + "]: " + msg)
  }
}

object UtilFunctions {
  
  def randomPositionInSpace(space: (Double,Double,Double,Double,Boolean)): Point = {
    val rnd = new Random()
    var x_inital = space._1
    var x_final = space._2
    var y_inital = space._3
    var y_final = space._4
    var lat = x_inital + rnd.nextInt(x_final.toInt - x_inital.toInt)
    var lng = y_inital + rnd.nextInt(y_final.toInt - y_inital.toInt)
    return Point(lat,lng)
  }
  
  def randomPositionsInSpace(space: (Double,Double,Double,Double,Boolean), n_player: Int): Array[Point] = {
    val rnd = new Random()
    var x_inital = space._1
    var x_final = space._2
    var y_inital = space._3
    var y_final = space._4
    var pos = new Array[Point](n_player)
    for(i <- 0 to (n_player-1)){
      var lat = x_inital + rnd.nextInt(x_final.toInt - x_inital.toInt)
      var lng = y_inital + rnd.nextInt(y_final.toInt - y_inital.toInt)
      pos(i) = Point(lat,lng)
    }
    return pos 
  }
  
  def randomPositionAroundPoint(point : Point) : Point = {
    var icon_size = ConfigFactory.load().getInt("icon_size")
    var treasure_radius = ConfigFactory.load().getInt("treasure_radius")
    var space_height = ConfigFactory.load().getDouble("space_height")
    var space_width = ConfigFactory.load().getDouble("space_width")
    
    var tmp_dist = treasure_radius - 20
    
    val rnd = new Random()
    var lat_rnd = rnd.nextInt(tmp_dist) - rnd.nextInt(tmp_dist)
    var lng_rnd = rnd.nextInt(tmp_dist) - rnd.nextInt(tmp_dist)
    var lat = point.x + lat_rnd
    var lng = point.y + lng_rnd
    
    val min_lat = icon_size
    val max_lat = space_width - icon_size
    if(lat < min_lat) lat = min_lat
    if(lat > max_lat) lat = max_lat
    
    val min_lng = icon_size
    val max_lng = space_height - icon_size
    if(lng < min_lng) lng = min_lng
    if(lng > max_lng) lng = max_lng
    
    Point(lat,lng)
  }
  
  def createSpaces(number : Int ): Array[(Double,Double,Double,Double,Boolean)] = {
    
    var icon_size = ConfigFactory.load().getDouble("icon_size")
    var ghost_radius = ConfigFactory.load().getDouble("ghost_radius")
    var treasure_radius = ConfigFactory.load().getDouble("treasure_radius")
    var space_height = ConfigFactory.load().getDouble("space_height")
    var space_width = ConfigFactory.load().getDouble("space_width")
    
    //divido sempre lo spazio totale per un numero pari di spaces dove in ognuno andrà un tesoro e in uno i giocatori
    // che sono sempre pari
    var nro_spaces = 0
    if(number % 2 > 0){
      nro_spaces = number + 1
    }else{
      nro_spaces = number
    }
    
    var column = 0
    //se il numero degli spazi è divisibile per 3 faccio 3 colonne, se no 2; definisco la grandezza di ogni spazio
    if((nro_spaces%3) == 0){
      column = 3
      space_width = (space_width-(icon_size *2)) / (3)
      space_height = (space_height-(icon_size)) / (nro_spaces/3)
    }else{
      column = 2
      space_width = (space_width-(icon_size *2)) / (2)
      space_height = (space_height-(icon_size)) / (nro_spaces/2)
    }
    
    var spaces = new Array[(Double, Double, Double, Double, Boolean)](nro_spaces)
    
    var i = 0
    var j = 0
    var count = 0
    //inizializzo le dimensioni per lo space iniziale
    var width_start = icon_size
    var height_start = icon_size
    var height_finish = space_height
    
    var width_finish = 0.0 //mi serve solo inizializzarla
    
    for(i <- 0 to (nro_spaces / column.toInt) - 1){
      for(j <- 0 to column.toInt - 1){
//        if (i == 0 && j == 0){
//          width_start = icon_size
//          height_start = icon_size
//          width_finish = space_width
//          height_finish = space_height
//        }else{
//          width_start = width_finish
//          width_finish = width_finish + space_width
//        }
        if (i == 0 && j == 0){
          width_finish = icon_size
        }
        width_start = width_finish
        width_finish = width_finish + space_width
        
        spaces(count) = (width_start, width_finish, height_start, height_finish, false)
        count = count + 1
      }
      height_start = height_finish
      height_finish = height_finish + space_height
      width_finish = icon_size
    }
    
    return spaces
    
  }
  
}