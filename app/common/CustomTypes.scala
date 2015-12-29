package common

sealed case class Point(x: Int, y: Int)
sealed case class Polygon(points: List[Point])