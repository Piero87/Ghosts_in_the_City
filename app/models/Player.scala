package models

import org.joda.time.DateTime
import play.api.libs.json.{ Json, Format }

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import play.api.Play.current
import play.api.db.DBApi
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

case class Player(
  id: Option[Long],
  name: String,
  team_id: Int,
  money: Int,
  latitude: Float,
  longitude: Float
)

object Player{
  implicit val format: Format[Player] = Json.format[Player]

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](current)
  import dbConfig._
  import dbConfig.driver.api._

  class PlayerTable(tag: Tag) extends Table[Player](tag, "PLAYER") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def team_id = foreignKey("TEAM", id, Team.table)(_.id)
    def money = column[Int]("MONEY")
    def latitude = column[Float]("LATITUDE")
    def longitude = column[Float]("LONGITUDE")
    
    def * = (id.?, name, team_id, money, latitude, longitude) 
            <> ((Player.apply _).tupled, Player.unapply)
  }
  
  val table = TableQuery[PlayerTable]
  
  def list: Future[Seq[Item]] = {
      val playerList = table.result
      db.run(playerList)
   }
  
  def getByID(playerID: Long): Future[Option[Player]] = {
      val playerByID = table.filter { f =>
        f.id === playerID
      }.result.headOption

      db.run(playerByID)
   }
  
  def create(newPlayer: Player): Future[Player] = {
    val insertion = (table returning table.map(_.id)) += newPlayer

    val insertedIDFuture = db.run(insertion)

    val createdCopy: Future[Player] = insertedIDFuture.map { resultID =>
      newPlayer.copy(id = Option(resultID))
    }

    createdCopy
  }
  
}