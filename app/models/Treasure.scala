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

case class Treasure (
  id: Option[Long],
  closed: Int,
  key_id: Long,
  money: Int,
  item_id: Long,
  latitude: Float,
  longitude: Float,
  game_id: Long
)

object Treasure {
  implicit val format: Format[Ghost] = Json.format[Ghost]

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](current)
  import dbConfig._
  import dbConfig.driver.api._

  class GhostsTable(tag: Tag) extends Table[Ghost](tag, "GHOSTS") {

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def closed = column[Int]("CLOSED")
    def key_id = foreignKey("KEY", id, Item.table)(_.id)
    def money = column[Int]("MONEY")
    def item_id = foreignKey("ITEM", id, Item.table)(_.id)
    def latitude = column[Float]("LATITUDE")
    def longitude = column[Float]("LONGITUDE")
    def game_id = foreignKey("GAME", id, Game.table)(_.id)

    def key_id = foreignKey("KEY", id, Item.table)(_.id)
    
    def * = (id.?, closed, money, latitude, longitude) <>
      ((Treasure.apply _).tupled, Treasure.unapply)
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