package backend.actors.models

/**
 * Class to create a Key
 */
sealed case class Key(uid: String) {
  
  var key_uid = uid
  var exist_key = true
  
  def getKeyUID = {
    key_uid
  }
  
  def existKey = {
    exist_key
  }
  
  def setExistKey(exist: Boolean) = {
    exist_key = exist
  }
}