package common

import scala.collection.mutable

class PointOutOfPolygonException(message: String = null, cause: Throwable = null) extends RuntimeException(message, cause)

sealed case class Point(latitude: Double, longitude: Double){
  
  // latitude == x
  // longitude == y
  
  def distanceFrom(p: Point): Double = {
    Math.sqrt(Math.pow((p.latitude - latitude),2) + Math.pow((p.longitude - longitude),2))
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
  def createVertexWithNewLat(p: Point, d: Double): Point = {
    val r_earth = 6371
    var new_lat = p.latitude + (d/r_earth) * (180/Math.PI)
    var vertex = new Point(new_lat,p.longitude)
    vertex
  }
  def createVertexWithNewLong(p: Point, d: Double): Point = {
    val r_earth = 6371
    var new_lon = p.longitude + (d/r_earth) * (180/Math.PI) / Math.cos(p.latitude * Math.PI/180)
    var vertex = new Point(p.latitude,new_lon)
    vertex
  }
  def createVertexWithNewLatLong(p: Point, d: Double): Point = {
    val r_earth = 6371
    var new_lat = p.latitude + (d/r_earth) * (180/Math.PI)
    var new_lon = p.longitude + (d/r_earth) * (180/Math.PI) / Math.cos(p.latitude * Math.PI/180)
    var vertex = new Point(new_lat,new_lon)
    vertex
  }
}

/*
 * O--------------------------A
 * |													|
 * |													|
 * |													|
 * C--------------------------B
 * 
 * O = origin
 */

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
