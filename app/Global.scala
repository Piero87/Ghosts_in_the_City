import play.api._
import backend.Backend

/**
 * Class that represent anything that needs to happen on start up.
 * It start frontend and backed application simultaneously and specifying 
 * the backend listening port
 */
object Global extends GlobalSettings{
  
  override def onStart(app: Application) {
    Logger.info("Applicazione partita")  
    Backend.main(Array("2551"))
  }
}