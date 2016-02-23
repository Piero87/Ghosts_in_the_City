package backend.actors.models

/**
 * Class to create a Gold
 */
sealed case class Gold(quantity: Int) {
  
  var amount = quantity
  
  def getAmount = {
    amount
  }
  
  def setAmount(newQuantity: Int) {
    amount = newQuantity
  }
   
}