package common

import scala.util.Random
import com.typesafe.config.ConfigFactory

object UtilFunctions {
  
  var icon_size = 0.0 //dimesione spazio livero nell'angolo
  var column = 0.0 //numero colonne
  var ghost_radius = 0.0
  var treasure_radius = 0.0
  var space_height = 0.0 
  var space_width = 0.0
  
  def randomPositionTreasure(space: (Int,Int,Int,Int,Boolean)): (Double,Double) = {
    val rnd = new Random()
    var lat = space._1 + rnd.nextInt( space._2 - space._1 +1)
    var lng = space._3 + rnd.nextInt( space._4 - space._3 +1)
    return (lat,lng)
  }
  
  def randomPositionPlayers(space: (Int,Int,Int,Int,Boolean), n_player: Int): Array[(Double,Double)] = {
    val rnd = new Random()
    var pos = new Array[(Double,Double)](n_player)
    var i = 0
    for(i <- 0 to (n_player-1)){
      //System.out.println(i)
      var lat = space._1 + rnd.nextInt( space._2 - space._1 +1)
      var lng = space._3 + rnd.nextInt( space._4 - space._3 +1)
      pos(i) = (lat,lng)
      System.out.println("position player "+i+": ("+pos(i)._1+", "+pos(i)._2+")")
    }
    return pos 
  }
  
  def randomPositionGhosts(space: (Int,Int,Int,Int,Boolean), pos_treasure : (Double,Double)) : (Double,Double) = {
    val rnd = new Random()
    var lat_rnd = (treasure_radius / 2) + rnd.nextInt(treasure_radius.toInt / 2)
    var lng_rnd = (treasure_radius / 2) + rnd.nextInt(treasure_radius.toInt / 2)
    var lat = pos_treasure._1 + lat_rnd
    var lng = pos_treasure._2 + lng_rnd
    if(lat < icon_size) lat = icon_size
    if(lat > space_height - icon_size) lat = space_height - icon_size
    if(lng < icon_size) lng = icon_size
    if(lng > space_height - icon_size) lng = space_height - icon_size
    (lat,lng)
  }
  
  def createSpaces(n_treasure : Int ): Array[(Double,Double,Double,Double,Boolean)] = {
    icon_size = ConfigFactory.load().getDouble("icon_size")
    ghost_radius = ConfigFactory.load().getDouble("ghost_radius")
    treasure_radius = ConfigFactory.load().getDouble("treasure_radius")
    space_height = ConfigFactory.load().getDouble("space_height")
    space_width = ConfigFactory.load().getDouble("space_width")
    
    //divido sempre lo spazio totale per un numero pari di spaces dove in ognuno andrà un tesoro e in uno i giocatori
    // che sono sempre pari
    var nro_spaces = 0
    if(n_treasure % 2 > 0){
      nro_spaces = n_treasure + 1
    }else{
      nro_spaces = n_treasure
    }
    
    //se il numero degli spazi è divisibile per 3 faccio 3 colonne, se no 2; definisco la grandezza di ogni spazio
    if((nro_spaces%3) == 0){
      column = 3
      space_width = (space_width-(icon_size *2)) / (3)
      space_height = (space_height-(icon_size)) / (nro_spaces/3)
    }else{
      column = 2
      space_width = (space_width-(icon_size *2)) / (2)
      space_height = (space_height-(icon_size)) / (nro_spaces/2)
    }
    
    var spaces = new Array[(Double, Double, Double, Double, Boolean)](nro_spaces)
    
    var i = 0
    var j = 0
    var count = 0
    //inizializzo le dimensioni per lo space iniziale
    var width_start = icon_size
    var height_start = icon_size
    var height_finish = space_height
    
    var width_finish = 0.0 //mi serve solo inizializzarla
    
    for(i <- 0 to (nro_spaces / column.toInt) - 1){
      for(j <- 0 to column.toInt - 1){
//        if (i == 0 && j == 0){
//          width_start = icon_size
//          height_start = icon_size
//          width_finish = space_width
//          height_finish = space_height
//        }else{
//          width_start = width_finish
//          width_finish = width_finish + space_width
//        }
        if (i == 0 && j == 0){
          width_finish = icon_size
        }
        width_start = width_finish
        width_finish = width_finish + space_width
        
        spaces(count) = (width_start, width_finish, height_start, height_finish, false)
        count = count + 1
      }
      height_start = height_finish
      height_finish = height_finish + space_height
      width_finish = icon_size
    }
    
    return spaces
    
  }
  
}