package be.angelcorp.omicronai.agents

import be.angelcorp.omicronai.assets.Asset
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.Location
import com.lyndir.omnicron.api.model.Player

class Soldier( val aiPlayer: Player, val asset: Asset ) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def receive = {
    case ExecuteActions( actions ) => actions.takeWhile( _.performAction( aiPlayer, this ) )
    case GetAsset() => sender ! asset
    case msg => logger.info( s"Asset received an unknown message: $msg" )
  }

  def relay( msg: SoldierMessage ) { context.parent ! msg }

}

sealed abstract class SoldierMessage
case class LocationObserved(location: Location) extends SoldierMessage
case class GetAsset()                           extends SoldierMessage
