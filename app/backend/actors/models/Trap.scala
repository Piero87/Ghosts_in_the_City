package backend.actors.models

import common._

/**
 * Class to create a Trap
 */
class Trap(p: Point) {
  
  var uid = scala.util.Random.alphanumeric.take(8).mkString
  var pos = p
  var status = TrapStatus.IDLE
  var trapped_ghost_uid = ""
  
  def getTrapInfo = {
    new TrapInfo(uid, pos, status)
  }
  
}