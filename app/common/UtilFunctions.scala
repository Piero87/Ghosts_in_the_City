package common

import scala.util.Random
import com.typesafe.config.ConfigFactory
import play.api.Logger

/**
 * Custom logger.
 */
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

/**
 * Util function object.
 * It contains most of the function used during the creation of a game 
 */
object UtilFunctions {
  
  val EARTH_RADIUS = 6371010; // metres
  
  private val MAX_ATTEMPTS = 100
  private val logger = new CustomLogger("UtilFunctions")
  
  def metersToLatitudeDelta(meters: Double): Double = {
    Math.toDegrees(meters/EARTH_RADIUS)
  }
  
  def metersToLongitudeDelta(meters: Double, latitude_radians: Double): Double = {
    Math.toDegrees( (meters/EARTH_RADIUS) / Math.cos(latitude_radians) )
  }
  
  /**
   * randomPositionInSpace method
   * It calculate a position(point with lat and lng) randomly in a given game area space
   * 
   * @param rect_space
   * @param permitted_area
   * @param margin
   */
  def randomPositionInSpace(rect_space: Rectangle, permitted_area: Polygon, margin: Double): Point = {
    logger.log("Space: " + rect_space)
    var attemps = 0
    val rnd = new Random()
    var point : Point = null
    do {
      var lat = rect_space.origin.latitude + ( (rect_space.width-margin) * rnd.nextDouble() )
      var lng = rect_space.origin.longitude + ( (rect_space.height-margin) * rnd.nextDouble() )
      point = new Point(lat,lng)
      logger.log("randomPositionInSpace - attempt: " + attemps + ", point: " + point)
      attemps += 1
    } while (!permitted_area.contains(point) && attemps != MAX_ATTEMPTS)
    if (attemps == MAX_ATTEMPTS){
      throw new PointOutOfPolygonException("from randomPositionInSpace")
    }
    logger.log("randomPositionInSpace - point: " + point)
    return point
  }
  
  /*
	def randomPositionsInSpace(rect_space: Rectangle, permitted_area: Polygon, n_positions: Int): Array[Point] = {
    var attemps = 0
    var pos = new Array[Point](n_positions)
    val rnd = new Random()
    for(i <- 0 to n_positions-1){
      attemps = 0
      var point : Point = null
      do {
        var lat = rect_space.origin.latitude + ( rect_space.width * rnd.nextDouble() )
        var lng = rect_space.origin.longitude + ( rect_space.height * rnd.nextDouble() )
        point = new Point(lat,lng)
        attemps += 1
      } while (!permitted_area.contains(point) && attemps != MAX_ATTEMPTS)
      if (attemps == MAX_ATTEMPTS){
        throw new PointOutOfPolygonException("from randomPositionsInSpace")
      }
      pos(i) = point
    }
    return pos
  }
  */
  
  /**
   * randomPositionAroundPoint method
   * It calculate a position(point with lat and lng) randomly around a given target point.
   * It will used to position a ghost who guard a treasure.
   * 
   * @param target_point
   * @param radius
   * @param permitted_area
   * @param game_type
   */
  def randomPositionAroundPoint(target_point: Point, radius: Double, permitted_area: Polygon, game_type: String) : Point = {
    var attemps = 0
    val rnd = new Random()
    var point : Point = null
    
    var lat_delta = radius
    var lng_delta = radius
    
    if (game_type == "reality") {
      lat_delta = metersToLatitudeDelta(radius)
      lng_delta = metersToLongitudeDelta(radius, target_point.latitude_rad)
    }
    
    do {
      point = new Point(target_point.latitude + rnd.nextDouble() * lat_delta, target_point.longitude + rnd.nextDouble() * lng_delta)
      logger.log("randomPositionAroundPoint - attempt: " + attemps + ", point: " + point)
      attemps += 1
    } while (!permitted_area.contains(point) && attemps != MAX_ATTEMPTS)
    if (attemps == MAX_ATTEMPTS){
      logger.log("randomPositionAroundPoint - safety_check failed")
      throw new PointOutOfPolygonException("from randomPositionAroundPoint")
    }
    logger.log("randomPositionAroundPoint - point: " + point)
    return point
  }
  
  /**
   * createSpaces method.
   * It divides the game area in rectangular spaces
   *
   * @param number
   * @param area 
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