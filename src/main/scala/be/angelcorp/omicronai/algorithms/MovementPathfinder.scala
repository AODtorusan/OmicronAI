package be.angelcorp.omicronai.algorithms

import math._
import scala.languageFeature.implicitConversions
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.Location._
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.Settings.settings

class MovementPathfinder( destination: Location, asset: Asset ) extends AStar {

  def heuristic(fromTile: Location) = fromTile δ destination

  def costOnto(fromTile: Location, toTile: Location) = {
    val baseCost = asset.mobility match {
      case Some(m) =>
        if ( fromTile.h == toTile.h )
          m.costForMovingInLevel( fromTile.h ) +
            settings.pathfinder.layerPenalty( toTile.h )
        else {
          m.costForLevelingToLevel( toTile.h ) +
            settings.pathfinder.layerChangePenalty +
            settings.pathfinder.layerPenalty( toTile.h )
        }
      case _ => Double.PositiveInfinity
    }
    abs(fromTile δ toTile).toDouble * baseCost
  }

  def goalReached(solution: AStarSolution) = destination == solution.tile

}
