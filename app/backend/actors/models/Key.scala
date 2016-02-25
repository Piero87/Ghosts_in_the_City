package backend.actors.models

/**
 * Class to create a Key
 */
sealed case class Key(uid: String) {
  
  var key_uid = uid
  
  def getKeyUID = {
    key_uid
  }
}