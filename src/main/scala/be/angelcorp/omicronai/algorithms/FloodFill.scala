package be.angelcorp.omicronai.algorithms

import be.angelcorp.omicronai._

object FloodFill {

  def fill( destination: Location ): Field[Double] = {
    val tiles = Field.fill(destination.size)(Double.PositiveInfinity)
    val cost  = Field.fill(destination.size)(1.0)
    fill(destination, new WorldMap(tiles, cost, cost, cost, cost))
  }

  def fill( destination: Location, map: WorldMap[Double, Double] ): Field[Double] = {
    map.tiles(destination) = 0
    fillFromTile(destination, map)
    map.tiles
  }

  def fillFromTile( tile: Location, map: WorldMap[Double, Double]) {
    val cost = map.tiles(tile)
    for (neighbour <- tile.neighbours) {
        val oldCost = map.tiles(neighbour._2)
        val newCost = cost + map.edgeAt(tile, neighbour._1).get
        if (newCost < oldCost && !oldCost.isNaN) {
          map.tiles(neighbour._2) = newCost
          fillFromTile(neighbour._2, map)
      }
    }
  }

}
