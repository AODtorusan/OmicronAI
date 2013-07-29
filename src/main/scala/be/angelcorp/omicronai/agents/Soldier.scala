package be.angelcorp.omicronai.agents

import be.angelcorp.omicronai.assets.Asset
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.Location
import com.lyndir.omicron.api.model.Player
import akka.actor.Actor

class Soldier( val aiPlayer: Player, val asset: Asset ) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  logger.debug(s"Promoted asset ${asset.name} to a soldier")

  def act = {
    case ExecuteAction( action ) =>
      logger.debug(s"${asset.name} is executing action: $action")
      action.performAction( aiPlayer )
    case RevokeAction( action ) =>
      logger.debug(s"${asset.name} is skipping action: $action")
    case OverruleAction(old, nw) =>
      logger.debug(s"${asset.name} wanted to do the following with ($old) but was overruled to do: $nw")
      nw.performAction( aiPlayer )

    case Self() =>
      logger.debug(s"Soldier ${asset.name} was asked for itself by $sender")
      sender ! this

    case GetAsset() =>
      logger.debug(s"Soldier ${asset.name} was asked for its asset by $sender")
      sender ! asset

    case Name() =>
      //logger.trace(s"${asset.name} was asked for a its name by $sender")
      sender ! asset.name

    case ListMembers() =>
      sender ! context.children

    case msg =>
      logger.info( s"Asset received an unknown message: $msg" )
  }

  def relay( msg: SoldierMessage ) { context.parent ! msg }

}

sealed abstract class SoldierMessage
case class GetAsset()                           extends SoldierMessage
