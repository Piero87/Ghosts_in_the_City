package common

import scala.collection.mutable

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
  
  val A = new Point(origin.latitude + width, origin.longitude)
  val B = new Point(origin.latitude + width, origin.longitude + height)
  val C = new Point(origin.latitude, origin.longitude + height)
  val edges = List(origin, A, B, C)
  
  def contains(p: Point): Boolean = {
    if ((origin.latitude < p.latitude) && (B.latitude > p.latitude) &&
        (origin.latitude < p.longitude) && (B.longitude > p.longitude)) {
      return true
    }
    return false
  }
  
}

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
 
