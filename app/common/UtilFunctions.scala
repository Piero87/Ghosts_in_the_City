package common

import scala.util.Random
import com.typesafe.config.ConfigFactory
import play.api.Logger

class CustomLogger (o: String) {
  var origin = o
  def log(msg: String) {
    var milliseconds = System.currentTimeMillis()
    var seconds = "0" + (milliseconds / 1000) % 60;
    var minutes = "0" + ((milliseconds / (1000*60)) % 60);
    var hours = "0" + ((milliseconds / (1000*60*60)) % 24);
    Logger.info("[" + hours.takeRight(2) + ":" + minutes.takeRight(2) + ":" + seconds.takeRight(2) + " - " + origin + "]: " + msg)
  }
}

object UtilFunctions {
  /*
  def randomPositionInSpace(space: Rectangle): Point = {
    val rnd = new Random()
    var lat = space.origin.latitude + ( space.width * rnd.nextDouble() )
    var lng = space.origin.longitude + ( space.height * rnd.nextDouble() )
    return Point(lat,lng)
  }
  */
  val max_try = 100
  
  def randomPositionInSpace(rect_space: Rectangle, permitted_area: Polygon): Point = {
    var safety_check = max_try
    val rnd = new Random()
    var lat = rect_space.origin.latitude + ( rect_space.width * rnd.nextDouble() )
    var lng = rect_space.origin.longitude + ( rect_space.height * rnd.nextDouble() )
    var point : Point = null
    do {
      point = new Point(lat,lng)
      safety_check -= 1
    } while (!permitted_area.contains(point) || safety_check != 0)
    if (safety_check == 0){
      throw new PointOutOfPolygonException("from randomPositionInSpace")
    }
    return point
  }
  /*
  def randomPositionsInSpace(space: Rectangle, n_positions: Int): Array[Point] = {
    val rnd = new Random()
    var pos = new Array[Point](n_positions)
    for(i <- 0 to (n_positions-1)){
      var lat = space.origin.latitude + ( space.width * rnd.nextDouble() )
    var lng = space.origin.longitude + ( space.height * rnd.nextDouble() )
      pos(i) = Point(lat,lng)
    }
    return pos 
  }
  */
  def randomPositionsInSpace(rect_space: Rectangle, permitted_area: Polygon, n_positions: Int): Array[Point] = {
    var safety_check = max_try
    var pos = new Array[Point](n_positions)
    val rnd = new Random()
    for(i <- 0 to (n_positions-1)){
      safety_check = max_try
      var lat = rect_space.origin.latitude + ( rect_space.width * rnd.nextDouble() )
      var lng = rect_space.origin.longitude + ( rect_space.height * rnd.nextDouble() )
      var point : Point = null
      do {
        point = new Point(lat,lng)
        safety_check -= 1
      } while (!permitted_area.contains(point) || safety_check != 0)
      if (safety_check == 0){
        throw new PointOutOfPolygonException("from randomPositionsInSpace")
      }
      pos(i) = point
    }
    return pos
  }
  /*
  def randomPositionAroundPoint(point : Point) : Point = {
    var icon_size = ConfigFactory.load().getInt("icon_size")
    var treasure_radius = ConfigFactory.load().getInt("treasure_radius")
    var space_height = ConfigFactory.load().getDouble("space_height")
    var space_width = ConfigFactory.load().getDouble("space_width")
    
    var tmp_dist = treasure_radius - 20
    
    val rnd = new Random()
    var lat_rnd = rnd.nextInt(tmp_dist) - rnd.nextInt(tmp_dist)
    var lng_rnd = rnd.nextInt(tmp_dist) - rnd.nextInt(tmp_dist)
    var lat = point.latitude + lat_rnd
    var lng = point.longitude + lng_rnd
    
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
  */
  
  def randomPositionAroundPoint(point: Point, radius: Double, permitted_area: Polygon) : Point = {
    var safety_check = max_try
    val rnd = new Random()
    var point : Point = null
    do {
      point = new Point(point.latitude + rnd.nextDouble() * radius, point.longitude + rnd.nextDouble() * radius)
      safety_check -= 1
    } while (!permitted_area.contains(point) || safety_check != 0)
    if (safety_check == 0){
      throw new PointOutOfPolygonException("from randomPositionAroundPoint")
    }
    return point
  }
  
  /*
  def createSpaces(number : Int, area: Rectangle): Array[Rectangle] = {
    
    var nro_spaces = 0
    if(number % 2 > 0){
      nro_spaces = number + 1
    }else{
      nro_spaces = number
    }
    
    var rows = 0
    var space_width = 0.0
    var space_height = 0.0
    
    //se il numero degli spazi è divisibile per 3 faccio 3 colonne, se no 2; definisco la grandezza di ogni spazio
    if((nro_spaces % 3) == 0){
      rows = 3
    }else{
      rows = 2
    }
    var columns = nro_spaces / rows
    
    space_width = area.width / columns
    space_height = area.height / rows
    
    var spaces = new Array[Rectangle](nro_spaces)
    
    var index = 0
    //inizializzo le dimensioni per lo spazio iniziale
    var lat_start = 0
    var lng_start = 0
    
    for(col <- 0 to columns - 1){
      for(row <- 0 to rows - 1){
        spaces(index) = new Rectangle(new Point( (lat_start + (space_width * col)) , (lng_start + (space_height * row))), space_width, space_height)
        index += 1  
      }
    }
    
    return spaces
    
  }
  */
  def createSpaces(number : Int, area: Polygon): Array[Rectangle] = {
    
    var nro_spaces = 0
    if(number % 2 > 0){
      nro_spaces = number + 1
    }else{
      nro_spaces = number
    }
    
    var rows = 0
    var space_width = 0.0
    var space_height = 0.0
    
    //se il numero degli spazi è divisibile per 3 faccio 3 colonne, se no 2; definisco la grandezza di ogni spazio
    if((nro_spaces % 3) == 0){
      rows = 3
    }else{
      rows = 2
    }
    var columns = nro_spaces / rows
    
    space_width = area.getRectangleThatContainsPolygon().width / columns
    space_height = area.getRectangleThatContainsPolygon().height / rows
    
    var spaces = new Array[Rectangle](nro_spaces)
    
    var index = 0
    //inizializzo le dimensioni per lo spazio iniziale
    var lat_start = 0
    var lng_start = 0
    
    for(col <- 0 to columns - 1){
      for(row <- 0 to rows - 1){
        spaces(index) = new Rectangle(new Point((lat_start + (space_width * col)) , (lng_start + (space_height * row))), space_width, space_height)
        index += 1  
      }
    }
    
    return spaces
    
  }
}