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

  class TreasuresTable(tag: Tag) extends Table[Treasure](tag, "TREASURES") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def closed = column[Int]("CLOSED")
    def key_id = column[Long]("KEY_ID")
    def money = column[Int]("MONEY")
    def item_id = column[Long]("ITEM_ID")
    def latitude = column[Float]("LATITUDE")
    def longitude = column[Float]("LONGITUDE")
    def game_id = column[Long]("GAME_ID")

    def key_ref = foreignKey("KEY", key_id, Item.table)(_.id)
    def item_ref = foreignKey("ITEM", item_id, Item.table)(_.id)
    def game_ref = foreignKey("GAME", game_id, Game.table)(_.id)
    
    def * = (id.?, closed, key_id, money, item_id, latitude, longitude, game_id) <>
      ((Treasure.apply _).tupled, Treasure.unapply)
  }

  val table = TableQuery[TreasuresTable]

  def list: Future[Seq[Treasure]] = {
    val treasureList = table.result
    db.run(treasureList)
  }

  def getByID(treasureID: Long): Future[Option[Treasure]] = {
    val treasureByID = table.filter { f =>
      f.id === treasureID
    }.result.headOption

    db.run(treasureByID)
  }

  def create(newEquipment: Treasure): Future[Treasure] = {
    val insertion = (table returning table.map(_.id)) += newEquipment

    val insertedIDFuture = db.run(insertion)

    val createdCopy: Future[Treasure] = insertedIDFuture.map { resultID =>
      newEquipment.copy(id = Option(resultID))
    }

    createdCopy
  }
}