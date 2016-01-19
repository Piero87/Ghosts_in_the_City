package backend.actors.models

/**
 * Class to create a Gold
 */
class Gold(quantity: Int) {
  
  var amount = quantity
  
  def getAmount = {
    amount
  }
   
}