package be.angelcorp.omicronai.actions

import math._
import be.angelcorp.omicronai.Location
import com.lyndir.omicron.api.model.Player
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicronai.agents.Soldier
import be.angelcorp.omicronai.algorithms.AStar

case class MoveVia(path: Seq[Location]) extends Action {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  override lazy val toString = s"move to via path to " + path.last

  def performAction(aiPlayer: Player, soldier: Soldier): Boolean = {
    logger.debug( s"Moving asset ${soldier.asset} from ${soldier.asset.location} via path to ${path.last}: $path" )
    val currentLocation = soldier.asset.gameObject.getLocation: Location

    soldier.asset.mobility match {
      case Some(m) =>
        for (newLocation <- path) {
          if (!(newLocation adjacentTo currentLocation))
            logger.warn(s"Moving asset ${soldier.asset} by more than one tile at once (${soldier.asset.location δ newLocation}}), path is undefined and intermediate tile may not be scanned!")

          implicit val game = m.getGameObject.getLocation.getLevel.getGame
          if (m.move( aiPlayer, newLocation )) {
            logger.trace( s"Reporting updated observable tiles of asset ${soldier.asset} on ${soldier.asset.location}" )
            //for (tile <- soldier.asset.observableTiles) soldier.relay( LocationObserved(tile) )
          } else {
            logger.debug( s"Asset ${soldier.asset} cannot move to $newLocation (destination, ${path.tail}), out of speed" )
            return false
          }
        }
        true
      case None =>
        logger.warn( s"Tried to move object ${soldier.asset} to ${path.last}, but that unit cannot move (no mobility module)" )
        false
    }
  }
}
