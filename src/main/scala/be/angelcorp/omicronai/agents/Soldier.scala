package be.angelcorp.omicronai.agents

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.Location
import com.lyndir.omicron.api.model.Player
import be.angelcorp.omicronai.assets.Asset

class Soldier( val owner: Player, val name: String, val asset: Asset ) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  logger.debug(s"Promoted asset $name to a soldier")

  def act = {
    case SimulateAction( action ) =>
      logger.debug(s"$name is simulating action: $action")
      sender ! performAction( action, simulate = true )

    case ExecuteAction( action ) =>
      logger.debug(s"$name is executing action: $action")
      context.parent ! performAction( action, simulate = false )

    case RevokeAction( action ) =>
      logger.debug(s"$name is skipping action: $action")

    case OverruleAction(oldAction, newAction) =>
      logger.debug(s"$name wanted to do the following with ($oldAction) but was overruled to do: $newAction")
      context.parent ! performAction( newAction, simulate = false )

    case GetAsset() =>
      logger.debug(s"Soldier $name was asked for its asset by $sender")
      sender ! asset

    case msg =>
      logger.info( s"Asset received an unknown message: $msg" )
  }

  def performAction( action: Action, simulate: Boolean = false ): ActionResult = action match {
    case MoveTo(destination) =>
      val origin = asset.gameObject.getLocation: Location
      logger.debug( s"Moving $name from $origin to $destination (simulate=$simulate)" )

      origin δ destination match {
        case 0 =>
          logger.debug(s"Tried to move $name by 0 tiles (no move, simulate=$simulate)")
          ActionSuccess( action )
        case 1 =>
          asset.mobility match {
            case Some(m) =>
              implicit val game = m.getGameObject.getLocation.getLevel.getGame
              if (m.canMove( owner, destination )) {
                if (simulate || m.move( owner, destination )) {
                  ActionSuccess( action, asset.observableTiles.map( l => UpdateLocation(l) ) )
                } else {
                  ActionFailed( action, s"Asset $name cannot move to $destination" )
                }
              } else {
                ActionFailed( action, s"Asset $name cannot move to $destination, insufficient of speed", OutOfSpeed() )
              }
            case None =>
              ActionFailed( action, s"Tried to move object $name to $destination, but that unit cannot move (no mobility module)", MissingModule() )
          }
        case distance =>
          ActionFailed( action, s"Cannot moving asset $name by more than one tile at once ($distance)" )
      }
    case _ =>
      logger.warn(s"$name does not know how to execute action: $action")
      ActionFailed( action, s"Unknown action: $action" )

  }

}

sealed abstract class SoldierMessage
case class GetAsset()                           extends SoldierMessage

sealed abstract class Action
case class MoveTo( location: Location ) extends Action
