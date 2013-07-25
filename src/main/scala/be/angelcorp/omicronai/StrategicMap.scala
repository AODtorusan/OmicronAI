package be.angelcorp.omicronai

import com.lyndir.omicron.api.model.Size
import be.angelcorp.omicronai.goals.SquareArea
import scala.collection.mutable

class StrategicMap(val size: Size) {

  val information = Array.fill( size.getWidth, size.getHeight, 3)(new StrategicTile)

  val tiles = for( u <- 0 until size.getWidth;
                   v <- 0 until size.getWidth;
                   h <- 0 until 3) yield new Location(u, v, h, size)
}

class StrategicView(val map: StrategicMap, coarseNess: Int, val roi: SquareArea) {

  lazy val tiles = map.tiles.filter( tile => roi inArea tile )

}

class StrategicTile {

  val threatIndex:          Double = 0.0
  val threatConfidence:     Double = 0.0
  /** [Enemy unit info, probability] */
  val threats               = mutable.Map[Int, Double]()

  val resourcesIndex:       Double = 0.0
  val resourcesConfidence:  Double = 0.0
  /** [Resource, probability] */
  val resource               = mutable.Map[Int, Double]()

  val strategicIndex:       Double = 0.0
  val strategicConfidence:  Double = 0.0
  /** [Strategic info, probability] */
  val strategic             = mutable.Map[Int, Double]()

}