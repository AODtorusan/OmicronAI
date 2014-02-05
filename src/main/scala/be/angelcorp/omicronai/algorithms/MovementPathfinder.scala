package be.angelcorp.omicronai.algorithms

import math._
import scala.languageFeature.implicitConversions
import be.angelcorp.omicronai.{Direction, Location}
import be.angelcorp.omicronai.Location._
import be.angelcorp.omicronai.configuration.Configuration
import Configuration.config
import be.angelcorp.omicronai.assets.Asset
import com.lyndir.omicron.api.model.Tile
import com.lyndir.omicron.api.util.Maybe.Presence
import be.angelcorp.omicronai.world.WorldGraph

class MovementPathfinder( destination: Location, asset: Asset ) extends AStar {

  def heuristic(fromTile: Location) = fromTile δ destination

  val costGraph = new  WorldGraph[Null, Double] {
    val inaccessible = Double.MaxValue
    implicit val game = asset.owner.getController.getGameController.getGame
    override def tileAt(l: Location): Null = ???
    override def edgeAt(l: Location, d: Direction) = {
      val tile: Tile = l.neighbour(d).get
      asset.mobility match {
        case Some(m) if tile.checkContents().presence() != Presence.PRESENT  =>
          Some( if ( d.dh == 0 ) {
            m.costForMovingInLevel( l.h ) +
              config.pathfinder.layerPenalty( l.h )
          } else {
            m.costForLevelingToLevel( l.h ) +
              config.pathfinder.layerChangePenalty +
              config.pathfinder.layerPenalty( l.h + d.dh )
          } )
        case _ => None
      }
    }
  }

  def goalReached(solution: AStarSolution) = destination == solution.tile

}
