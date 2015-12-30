package common

import scala.util.Random

object UtilFunctions {
  
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
  
  def randomPositionGhosts(pos_treasure : (Double,Double)) : (Double,Double) = {
    val rnd = new Random()
    var lat_rnd = 50+ rnd.nextInt(50)
    var lng_rnd = 50 + rnd.nextInt(50)
    var lat = pos_treasure._1 + lat_rnd
    var lng = pos_treasure._2 + lng_rnd
    if(lat < 48) lat = 48
    if(lat > 450) lat = 450
    if(lng < 48) lng = 48
    if(lng > 450) lng = 450
    (lat,lng)
  }
  
  def createSpaces(n_treasure : Int, env_width:Int, env_height:Int ): Array[(Int,Int,Int,Int,Boolean)] = {
    //divido sempre lo spazio totale per un numero pari di spaces dove in ognuno andrà un tesoro e in uno i giocatori
    // che sono sempre pari
    var nro_spaces = 0
    if(n_treasure%2 > 0){
      nro_spaces = n_treasure+1
    }else{
      nro_spaces = n_treasure
    }
    
    var icon_dim = 48 //dimesione spazio livero nell'angolo 
    var column = 0  //nro colonne
    var height_space = 0 
    var width_space = 0
    
    //se il numero degli spazi è divisibile per 3 faccio 3 colonne, se no 2; definisco la grandezza di ogni spazio
    if((nro_spaces%3) == 0){
      column = 3
      width_space = (env_width-(icon_dim *2)) / (3)
      height_space = (env_height-(icon_dim)) / (nro_spaces/3)
    }else{
      column = 2
      width_space = (env_width-(icon_dim *2)) / (2)
      height_space = (env_height-(icon_dim)) / (nro_spaces/2)
    }
    
    System.out.println("width space = "+width_space)
    System.out.println("height space = "+height_space)
    System.out.println("column = "+column)
    
    var spaces = new Array[(Int, Int, Int, Int, Boolean)](nro_spaces)
    
    var i = 0
    var j = 0
    var count = 0
    //inizializzo le dimensioni per lo space iniziale
    var width_start = icon_dim
    var height_start = icon_dim
    var height_finish = height_space
    
    var width_finish = 0 //mi serve solo inizializzarla
    
    for(i <- 0 to (nro_spaces/column)-1){
      for(j <- 0 to column-1){
//        if (i == 0 && j == 0){
//          width_start = icon_dim
//          height_start = icon_dim
//          width_finish = width_space
//          height_finish = height_space
//        }else{
//          width_start = width_finish
//          width_finish = width_finish + width_space
//        }
        if (i == 0 && j == 0){
          width_finish = icon_dim
        }
        width_start = width_finish
        width_finish = width_finish + width_space
        
        spaces(count)= (width_start,width_finish, height_start, height_finish,false)
        System.out.println("space "+count+" = W_start "+spaces(count)._1+", W_end "+spaces(count)._2+", H_start "+spaces(count)._3+", H_end "+spaces(count)._4)
        count = count+1
      }
      height_start = height_finish
      height_finish = height_finish + height_space
      width_finish = icon_dim
    }
    
    return spaces
    
  }
  
}