package common

object Util {
  
  case class Point(x: Double, y: Double)

  type Polygon = List[Point]
  
  def calculateArea(polygon: Polygon, area: Double = 0) : Double =
  polygon.zip(polygon.tail.toStream #::: polygon.toStream).foldLeft(0.0)((area, points) =>
    area + (points._2.x + points._1.x) * (points._1.y - points._2.y)) / 2
}
