package backend.actors.models

/**
 * Class to create a Key
 */
class Key(uid: String) {
  
  var key_uid = uid

  def getKeyUID = {
    key_uid
  }
}