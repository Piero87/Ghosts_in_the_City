package backend.actors.models

/**
 * Class to create a Key
 */
class Key(isNecessary: Boolean, id: String) {
  
  var requested = isNecessary
  var key_id = id
   
}