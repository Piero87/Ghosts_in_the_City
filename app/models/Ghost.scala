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

case class Ghost (
  id: Option[Long],
  player_id: Long,
  item_id: Long,
  status: Int,
  latitude: Float,
  longitude: Float,
  treasure_id: Long
)

object Ghost {
  implicit val format: Format[Ghost] = Json.format[Ghost]

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](current)
  import dbConfig._
  import dbConfig.driver.api._

  class GhostsTable(tag: Tag) extends Table[Ghost](tag, "GHOSTS") {

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def player_id = column[Int]("PLAYER_ID")
    def item_ide = foreignKey("ITEM", id, Item.table)(_.id)
    def status = column[Int]("STATUS")
    def latitude = column[Float]("LATITUDE")
    def longitude = column[Float]("LONGITUDE")
    def treasure_id = foreignKey("TREASURE", id, Treasure.table)(_.id)


    def * = (id.?, player_id, treasure_id) <>
      ((Ghost.apply _).tupled, Ghost.unapply)
  }

  val table = TableQuery[GhostsTable]

  def list: Future[Seq[Ghost]] = {
    val ghostList = table.result
    db.run(ghostList)
  }

  def getByID(ghostID: Int): Future[Option[Ghost]] = {
    val ghostByID = table.filter { f =>
      f.id === ghostID
    }.result.headOption

    db.run(ghostByID)
  }

  def create(newEquipment: Ghost): Future[Ghost] = {
    val insertion = (table returning table.map(_.id)) += newEquipment

    val insertedIDFuture = db.run(insertion)

    val createdCopy: Future[Ghost] = insertedIDFuture.map { resultID =>
      newEquipment.copy(id = Option(resultID))
    }

    createdCopy
  }
}