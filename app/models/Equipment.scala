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

case class Equipment (
  id: Option[Long],
  player_id: Long,
  item_id: Long
)

object Equipment {
  implicit val format: Format[Equipment] = Json.format[Equipment]

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](current)
  import dbConfig._
  import dbConfig.driver.api._

  class EquipmentsTable(tag: Tag) extends Table[Equipment](tag, "EQUIPMENTS") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def player_id = column[Long]("PLAYER")
    def item_id = column[Long]("ITEM")
    
    def player_ref = foreignKey("PLAYER", player_id, Player.table)(_.id)
    def item_ref = foreignKey("ITEM", item_id, Item.table)(_.id)

    def * = (id.?, player_id, item_id) <>
      ((Equipment.apply _).tupled, Equipment.unapply)
  }

  val table = TableQuery[EquipmentsTable]

  def list: Future[Seq[Equipment]] = {
    val equipmentList = table.result
    db.run(equipmentList)
  }

  def getByID(equipmentID: Long): Future[Option[Equipment]] = {
    val equipmentByID = table.filter { f =>
      f.id === equipmentID
    }.result.headOption

    db.run(equipmentByID)
  }

  def create(newEquipment: Equipment): Future[Equipment] = {
    val insertion = (table returning table.map(_.id)) += newEquipment

    val insertedIDFuture = db.run(insertion)

    val createdCopy: Future[Equipment] = insertedIDFuture.map { resultID =>
      newEquipment.copy(id = Option(resultID))
    }

    createdCopy
  }
}