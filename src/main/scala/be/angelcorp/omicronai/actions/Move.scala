package be.angelcorp.omicronai.actions

import be.angelcorp.omicronai.Location
import com.lyndir.omnicron.api.model.Player
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.AStar
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicronai.agents.{LocationObserved, Soldier}

class MoveTo(val destination: Location) extends Action {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def performAction(aiPlayer: Player, soldier: Soldier) = {
    val currentLocation = soldier.asset.gameObject.getLocation: Location
    try{
      val path = AStar(destination).findPath( currentLocation ).path.reverse
      path.sliding(2).foldLeft(true)( (previousSuccess, nodes) => {
        if (previousSuccess) {
          val delta = nodes(1) - nodes(0)
          new MoveBy(delta).performAction(aiPlayer, soldier)
        } else false
      } )
    } catch {
      case e: Throwable =>
        logger.warn(s"Could not find a path for asset ${soldier.asset} from $currentLocation to $destination")
        false
    }
  }
}

class MoveBy(val du: Int, val dv: Int, val dh: Int) extends Action {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def this(delta: (Int, Int, Int)) = this( delta._1, delta._2, delta._3 )

  def performAction(aiPlayer: Player, soldier: Soldier) = {
    logger.trace( "Moving asset {} by {} from {}", Array(soldier.asset, (du, dv, dh), soldier.asset.location) )
    require( dh == 0 )
    soldier.asset.mobility match {
      case Some(m) =>
        m.move( aiPlayer, du, dv )

        logger.trace( "Reporting updated observable tiles of asset {} on {}", soldier.asset, soldier.asset.location )
        for (tile <- soldier.asset.observableTiles) soldier.relay( LocationObserved(tile) )
        true
      case _ =>
        logger.warn( s"Tried to move object ${soldier.asset} by ${(du, dv, dh)}, but that unit cannot move (no mobility module)" )
        false
    }
  }
}
