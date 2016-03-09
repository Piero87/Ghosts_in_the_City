package common

import scala.collection.mutable
import com.typesafe.config.ConfigFactory
import scala.util.Random

sealed case class GameParameters(game_type: String){
  
  // **** GAME ARENA ****
  
  val canvas_width = ConfigFactory.load().getDouble("canvas_width")
  val canvas_height = ConfigFactory.load().getDouble("canvas_height")
  val canvas_margin = if (game_type == GameType.REALITY) {
                          ConfigFactory.load().getDouble("real_margin") 
                      } else { 
                          ConfigFactory.load().getDouble("web_margin")
                      }
  
  
  // **** GHOSTS ****
  
  val ghost_step = if (game_type == GameType.REALITY) {
                            ConfigFactory.load().getDouble("real_ghost_step") 
                        } else { 
                            ConfigFactory.load().getDouble("web_ghost_step")
                        }
  val ghost_radius = if (game_type == GameType.REALITY) {
                          ConfigFactory.load().getDouble("real_ghost_radius") 
                      } else { 
                          ConfigFactory.load().getDouble("web_ghost_radius")
                      }
  val ghost_hunger = Array(0.0,
                           ConfigFactory.load().getDouble("ghost_hunger_level1"),
                           ConfigFactory.load().getDouble("ghost_hunger_level2"),
                           ConfigFactory.load().getDouble("ghost_hunger_level3"))
  
  // **** TREASURES ****
                           
  val treasure_radius = if (game_type == GameType.REALITY) {
                            ConfigFactory.load().getDouble("real_ghost_radius") 
                        } else { 
                            ConfigFactory.load().getDouble("web_ghost_radius")
                        }
  val min_treasure_gold = ConfigFactory.load().getInt("min_treasure_gold")
  val max_treasure_gold = ConfigFactory.load().getInt("max_treasure_gold")
  val ghosts_per_treasure = ConfigFactory.load().getInt("ghosts_per_treasure")
  
  // **** TRAP ****
  
  val trap_radius = if (game_type == GameType.REALITY) {
                        ConfigFactory.load().getDouble("real_trap_radius") 
                    } else { 
                        ConfigFactory.load().getDouble("web_trap_radius")
                    }
  
  // **** PLAYERS ****
  val initial_gold = ConfigFactory.load().getInt("initial_gold")
  
  // **** OTHERS ****
  val max_action_distance = if (game_type == GameType.REALITY) {
                                ConfigFactory.load().getDouble("real_max_action_distance") 
                            } else { 
                                ConfigFactory.load().getDouble("web_max_action_distance")
                            }
}


// Custom Exceptions

class PointOutOfPolygonException(message: String = null, cause: Throwable = null) extends RuntimeException(message, cause)

// Geometric and Spacial types

sealed case class Point(latitude: Double, longitude: Double){
  
  // latitude == x
  // longitude == y
  val latitude_rad = Math.toRadians(latitude)
  val longitude_rad = Math.toRadians(longitude)
  
  private def pixelsFrom(p: Point): Double = {
    Math.sqrt(Math.pow((p.latitude - latitude),2) + Math.pow((p.longitude - longitude),2))
  }
  
  private def metersFrom(p: Point): Double = {
    var R = 6371010; // metres
    var φ1 = Math.toRadians(latitude)
    var φ2 = Math.toRadians(p.latitude)
    var Δφ = Math.toRadians(p.latitude - latitude)
    var Δλ = Math.toRadians(p.longitude - longitude)
    
    var a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
            Math.cos(φ1) * Math.cos(φ2) *
            Math.sin(Δλ/2) * Math.sin(Δλ/2);
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    
    R * c;
  }
  
  def distanceFrom(p: Point, game_type: String): Double = {
    if (game_type == GameType.REALITY) {
      return metersFrom(p)
    } else {
      return pixelsFrom(p)
    }
  }
  
  private def angle_rad_to(p: Point): Double = {
    val delta_lat = p.latitude - latitude
    val delta_lng = p.longitude - longitude
    
    Math.atan2( delta_lng , delta_lat )
  }
  
  private def virtual_step_to(angle_rad: Double, pixels: Double): Point = {
    
    System.out.println("direction: " + Math.toDegrees(angle_rad))
    
    val new_lat = latitude + Math.cos( angle_rad ) * pixels
    val new_lng = longitude + Math.sin( angle_rad ) * pixels
    
    new Point(new_lat, new_lng)
    
  }
  
  private def bearing_rad_to(p: Point): Double = {
    
    val delta_lng = p.longitude - longitude
    
    Math.atan2( Math.sin(delta_lng) * Math.cos(p.latitude_rad),
                Math.cos(latitude_rad) * Math.sin(p.latitude_rad) - Math.sin(latitude_rad) * Math.cos(p.latitude_rad)*Math.cos(delta_lng))
  }
  
  private def real_step_to(bearing_rad: Double, meters: Double): Point = {
    
    // lat2: =ASIN(SIN(lat1)*COS(d/R) + COS(lat1)*SIN(d/R)*COS(brng))
    // lon2: =lon1 + ATAN2(COS(d/R)-SIN(lat1)*SIN(lat2), SIN(brng)*SIN(d/R)*COS(lat1))
    
    val R = UtilFunctions.EARTH_RADIUS;
    
    val new_latitude_rad = Math.asin( Math.sin(latitude_rad) * Math.cos( meters / R ) +
                                      Math.cos(latitude_rad) * Math.sin( meters / R ) * Math.cos(bearing_rad) )
    val new_longitude_rad = longitude + Math.atan2( Math.cos( meters / R ) - Math.sin(latitude_rad) * Math.sin(new_latitude_rad), 
                                                    Math.cos(latitude_rad) * Math.sin( meters / R ) * Math.cos(bearing_rad) )
    
    new Point( Math.toDegrees(new_latitude_rad), Math.toDegrees(new_longitude_rad) )
    
  }
  
  def stepTowards(p: Point, length: Double, game_type: String): Point = {
    
    if (game_type == GameType.REALITY) {
      return real_step_to(angle_rad_to(p), length)
    } else {
      return virtual_step_to(bearing_rad_to(p), length)
    }
    
  }
  
  def randomStep(length: Double, game_type: String): Point = {
    
    val rnd = new Random()
    val radians = rnd.nextFloat() * 2 * Math.PI;
    
    if (game_type == GameType.REALITY) {
      return real_step_to(radians, length)
    } else {
      return virtual_step_to(radians, length)
    }
    
  }
  
  def convertToReality(canvas: Rectangle, reality: Rectangle) : Point = {
    // w_canvas : lat_canvas = w_reality : lat_reality
    var reality_lat = (latitude * reality.width) / canvas.width
    // h_canvas : lng_canvas = h_reality : lng_reality
    var reality_lng = (longitude * reality.height) / canvas.height
    new Point(reality_lat, reality_lng)
  }
  
  def convertToCanvas(canvas: Rectangle, reality: Rectangle) : Point = {
    // w_canvas : lat_canvas = w_reality : lat_reality
    var canvas_lat = (latitude * reality.width) / canvas.width
    // h_canvas : lng_canvas = h_reality : lng_reality
    var canvas_lng = (longitude * reality.height) / canvas.height
    new Point(canvas_lat, canvas_lng)
  }
  
}

object Vertex {
  
  def createVertexWithNewLat(p: Point, dist_meters: Double): Point = {
    
    val r_earth = UtilFunctions.EARTH_RADIUS
    val delta_lat = Math.toDegrees(dist_meters/r_earth)
    
    val new_lat = p.latitude + delta_lat
    return new Point(new_lat,p.longitude)
    
  }
  
  def createVertexWithNewLong(p: Point, dist_meters: Double): Point = {
    
    val r_earth = UtilFunctions.EARTH_RADIUS
    val delta_lng = Math.toDegrees( (dist_meters/r_earth) / Math.cos(p.latitude_rad) )
    
    val new_lng = p.longitude + delta_lng
    return new Point(p.latitude,new_lng)
    
  }
  
  def createVertexWithNewLatLong(p: Point, dist_meters: Double): Point = {
    
    val r_earth = UtilFunctions.EARTH_RADIUS
    val delta_lat = Math.toDegrees(dist_meters/r_earth)
    val delta_lng = Math.toDegrees( (dist_meters/r_earth) / Math.cos(p.latitude_rad) )
    
    val new_lat = p.latitude + delta_lat
    val new_lng = p.longitude + delta_lng
    return new Point(new_lat,new_lng)
    
  }
  
}

sealed case class Rectangle(origin: Point, width: Double, height: Double){
  
  val TopLeftCorner = origin
  val TopRightCorner = new Point(origin.latitude + width, origin.longitude)
  val BottomRightCorner = new Point(origin.latitude + width, origin.longitude + height)
  val BottomLeftCorner = new Point(origin.latitude, origin.longitude + height)
  val edges = List(origin, TopRightCorner, BottomRightCorner, BottomLeftCorner)
  
  def contains(p: Point): Boolean = {
    if ((origin.latitude < p.latitude) && (BottomRightCorner.latitude > p.latitude) &&
        (origin.latitude < p.longitude) && (BottomRightCorner.longitude > p.longitude)) {
      return true
    }
    return false
  }
  
}

sealed case class Polygon(vertex: List[Point]){
  
  def contains(point: Point): Boolean = {
    val vertex_closed = vertex :+ vertex.head
    val n_vertex = vertex_closed.length
    val latitudes : List[Double] = vertex_closed.map { x => x.latitude }
    val longitudes : List[Double] = vertex_closed.map { x => x.longitude }
    var contained = false
    var i = 0
    var j = n_vertex - 1
    while (i < n_vertex) {
      if (((longitudes(i) > point.longitude) != (longitudes(j) > point.longitude)) && 
          (point.latitude < (latitudes(j) - latitudes(i)) * (point.longitude - longitudes(i)) / (longitudes(j) - longitudes(i)) + latitudes(i))){
        contained = !contained;
      }
      j = i
      i += 1
    }
    contained
  }
  
  def getRectangleThatContainsPolygon(): Rectangle = {
    
    val latitudes = vertex.map { x => x.latitude }
    val min_lat = latitudes.reduceLeft(_ min _)
    val max_lat = latitudes.reduceLeft(_ max _)
    
    val longitudes = vertex.map { y => y.longitude }
    val min_lng = longitudes.reduceLeft(_ min _)
    val max_lng = longitudes.reduceLeft(_ max _)
    
    new Rectangle(new Point(min_lat, min_lng), (max_lat - min_lat), (max_lng - min_lng))
  }
  
}
/*
sealed case class Polygon(points: List[Point]){
  
  val polygonPoints = points
  val edges = foundEdge
  
  // Create Seq[Edge] from polygon list point
  def foundEdge : Seq[Edge] = {
    val edges = mutable.ArrayBuffer[Edge]() 
    for(i <- 0 to polygonPoints.length-1){
      if(i == polygonPoints.length-1){
         var edge = new Edge(polygonPoints(i), polygonPoints(0))
        edges.append(edge)
      }else{
        var edge = new Edge(polygonPoints(i), polygonPoints(i+1))
        edges.append(edge)
      }
    }
    edges
  }
  
  def contains(p: Point) = edges.count(_.raySegI(p)) % 2 != 0
}

case class Edge(_1: Point, _2: Point) {
  
  import Math._
  import Double._
 
  // Ray-casting algorithm
  // It checks if a point is contained in the Polygon
  def raySegI(p: Point): Boolean = {
    if (_1.longitude > _2.longitude) return Edge(_2, _1).raySegI(p)
    if (p.longitude == _1.longitude || p.longitude == _2.longitude) return raySegI(new Point(p.latitude, p.longitude + epsilon))
    if (p.longitude > _2.longitude || p.longitude < _1.longitude || p.latitude > max(_1.latitude, _2.latitude))
      return false
    if (p.latitude < min(_1.latitude, _2.latitude)) return true
    val blue = if (abs(_1.latitude - p.latitude) > MinValue) (p.longitude - _1.longitude) / (p.latitude - _1.latitude) else MaxValue
    val red = if (abs(_1.latitude - _2.latitude) > MinValue) (_2.longitude - _1.longitude) / (_2.latitude - _1.latitude) else MaxValue
    blue >= red
  }
 
  final val epsilon = 0.00001
}
*/
