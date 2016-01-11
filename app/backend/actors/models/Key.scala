package backend.actors.models

/**
 * Class to create a Key
 */
class Key(id: String) {
  
  var key_id = id

  def getKeyID = {
    key_id
  }
}

//class Empty (override val id: String) extends Key (id) {
//  
//}