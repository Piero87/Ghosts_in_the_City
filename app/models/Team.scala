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

case class Team (
  id: Option[Long],
  name: String,
  color: String,
  game_id: Long
)

object Team {
  implicit val format: Format[Ghost] = Json.format[Ghost]

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](current)
  import dbConfig._
  import dbConfig.driver.api._

  class TeamsTable(tag: Tag) extends Table[Team](tag, "Teams") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def color = column[String]("COLOR")
    def game_id = column[Long]("GAME_ID")

    def game_ref = foreignKey("GAME", game_id, Game.table)(_.id)
    
    def * = (id.?, name, color, game_id) <>
      ((Team.apply _).tupled, Team.unapply)
  }

  val table = TableQuery[TeamsTable]

  def list: Future[Seq[Team]] = {
    val teamList = table.result
    db.run(teamList)
  }

  def getByID(teamID: Long): Future[Option[Team]] = {
    val teamByID = table.filter { f =>
      f.id === teamID
    }.result.headOption

    db.run(teamByID)
  }

  def create(newEquipment: Team): Future[Team] = {
    val insertion = (table returning table.map(_.id)) += newEquipment

    val insertedIDFuture = db.run(insertion)

    val createdCopy: Future[Team] = insertedIDFuture.map { resultID =>
      newEquipment.copy(id = Option(resultID))
    }

    createdCopy
  }
}