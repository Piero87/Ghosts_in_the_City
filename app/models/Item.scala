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

case class Item (
  id: Option[Long],
  //  da cambiare nome nel db perchè lo vede come un istruzione di scala
  i_type: String, 
  cost: Int,
  latitude: Float,
  longitude: Float,
  game_id: Int
)

object Item{
  implicit val format: Format[Item] = Json.format[Item]

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](current)
  import dbConfig._
  import dbConfig.driver.api._
  
  class ItemTable(tag: Tag) extends Table[Item](tag, "ITEM") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def i_type = column[String]("TYPE")
    def cost = column[Int]("COST")
    def latitude = column[Float]("LATITUDE")
    def longitude = column[Float]("LONGITUDE")
    def game_id = foreignKey("GAME", id, Game.table)(_.id)
    //non ho capito quel "GAME"
    
    def * = (id.?, i_type, cost, latitude, longitude, game_id) 
            <> ((Item.apply _).tupled, Item.unapply)
    //cosa fa la funzione <>?
  }
  
   val table = TableQuery[ItemTable]
   
   def list: Future[Seq[Item]] = {
      val itemList = table.result
      db.run(itemList)
   }
   
   def getByID(itemID: Long): Future[Option[Item]] = {
      val itemByID = table.filter { f =>
        f.id === itemID
      }.result.headOption

      db.run(itemByID)
   }
   
   def create(newItem: Item): Future[Item] = {
    val insertion = (table returning table.map(_.id)) += newItem

    val insertedIDFuture = db.run(insertion)

    val createdCopy: Future[Item] = insertedIDFuture.map { resultID =>
      newItem.copy(id = Option(resultID))
    }

    createdCopy
  }
}

