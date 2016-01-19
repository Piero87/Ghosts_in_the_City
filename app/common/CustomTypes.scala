package common

import scala.collection.mutable

sealed case class Point(x: Double, y: Double){
  
  var pos_x = x
  var pos_y = y
  
  def isNearby(p: Point, range: Double): Boolean = {
    var dist = Math.sqrt(Math.pow((p.x - pos_x),2) + Math.pow((p.y - pos_y),2))
    if (dist <= range) {
      true
    }
    false
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
    if (_1.y > _2.y) return Edge(_2, _1).raySegI(p)
    if (p.y == _1.y || p.y == _2.y) return raySegI(new Point(p.x, p.y + epsilon))
    if (p.y > _2.y || p.y < _1.y || p.x > max(_1.x, _2.x))
      return false
    if (p.x < min(_1.x, _2.x)) return true
    val blue = if (abs(_1.x - p.x) > MinValue) (p.y - _1.y) / (p.x - _1.x) else MaxValue
    val red = if (abs(_1.x - _2.x) > MinValue) (_2.y - _1.y) / (_2.x - _1.x) else MaxValue
    blue >= red
  }
 
  final val epsilon = 0.00001
}
 
