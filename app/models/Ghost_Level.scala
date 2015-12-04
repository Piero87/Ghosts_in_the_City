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

case class Ghost_Level (
  id: Option[Long],
  range_latitude: Int,
  range_longitude: Int,
  max_damage : Int
)

object Ghost_Level {
  implicit val format: Format[Ghost_Level] = Json.format[Ghost_Level]

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](current)
  import dbConfig._
  import dbConfig.driver.api._

  class Ghosts_LevelsTable(tag: Tag) extends Table[Ghost_Level](tag, "GHOSTS_LEVELS") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def range_latitude = column[Int]("RANGE_LATITUDE")
    def range_longitude = column[Int]("RANGE_LONGITUDE")
    def max_damage = column[Int]("MAX_DAMAGE")


    def * = (id.?, range_latitude, range_longitude, max_damage) <>
      ((Ghost_Level.apply _).tupled, Ghost_Level.unapply)
  }

  val table = TableQuery[Ghosts_LevelsTable]

  def list: Future[Seq[Ghost_Level]] = {
    val ghost_level_list = table.result
    db.run(ghost_level_list)
  }

  def getByID(ghost_level_ID: Long): Future[Option[Ghost_Level]] = {
    val ghost_level_by_ID = table.filter { f =>
      f.id === ghost_level_ID
    }.result.headOption

    db.run(ghost_level_by_ID)
  }

  def create(newGhost_Level: Ghost_Level): Future[Ghost_Level] = {
    val insertion = (table returning table.map(_.id)) += newGhost_Level

    val insertedIDFuture = db.run(insertion)

    val createdCopy: Future[Ghost_Level] = insertedIDFuture.map { resultID =>
      newGhost_Level.copy(id = Option(resultID))
    }

    createdCopy
  }
}