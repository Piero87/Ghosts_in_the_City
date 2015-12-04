package models

import org.joda.time.DateTime
import play.api.libs.json.{ Json, Format }

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import play.api.Play.current
import play.api.db.DBApi
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

import SlickMapping.jodaDateTimeMapping

case class Game (
  id: Option[Long],
  start: DateTime,
  end: DateTime,
  maxtime : Int,
  top_left_latitude: Float,
  top_left_longitude: Float,
  top_right_latitude: Float,
  top_right_longitude: Float,
  bottom_left_latitude: Float,
  bottom_left_longitude: Float,
  bottom_right_latitude: Float,
  bottom_right_longitude: Float
)

object Game {
  implicit val format: Format[Game] = Json.format[Game]

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](current)
  import dbConfig._
  import dbConfig.driver.api._

  class GamesTable(tag: Tag) extends Table[Game](tag, "GAMES") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def start = column[DateTime]("START")
    def end = column[DateTime]("END")
    def maxtime = column[Int]("MATIME")
    def top_left_latitude = column[Float]("TOP_LEFT_LATITUDE")
    def top_left_longitude = column[Float]("END")
    def top_right_latitude = column[Float]("END")
    def top_right_longitude = column[Float]("END")
    def bottom_left_latitude = column[Float]("END")
    def bottom_left_longitude = column[Float]("END")
    def bottom_right_latitude = column[Float]("END")
    def bottom_right_longitude = column[Float]("END")


    def * = (id.?, start, end, maxtime, top_left_latitude, top_left_longitude, top_right_latitude,top_right_longitude,bottom_left_latitude,bottom_left_longitude,bottom_right_latitude,bottom_right_longitude) <>
      ((Game.apply _).tupled, Game.unapply)
  }

  val table = TableQuery[GamesTable]

  def list: Future[Seq[Game]] = {
    val eventList = table.result
    db.run(eventList)
  }

  def getByID(gameID: Long): Future[Option[Game]] = {
    val gameByID = table.filter { f =>
      f.id === gameID
    }.result.headOption

    db.run(gameByID)
  }

  def create(newGame: Game): Future[Game] = {
    val insertion = (table returning table.map(_.id)) += newGame

    val insertedIDFuture = db.run(insertion)

    val createdCopy: Future[Game] = insertedIDFuture.map { resultID =>
      newGame.copy(id = Option(resultID))
    }

    createdCopy
  }
}