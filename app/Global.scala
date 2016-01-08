import play.api._
import backend.Backend

object Global extends GlobalSettings{
  
  override def onStart(app: Application) {
    Logger.info("Applicazione partita")  
    //Backend.main(Array("2551"))
  }
}