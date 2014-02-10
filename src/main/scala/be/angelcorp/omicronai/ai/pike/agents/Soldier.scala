package be.angelcorp.omicronai.ai.pike.agents

import akka.actor.{TypedProps, TypedActor, ActorRef}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.IGameObject
import be.angelcorp.omicronai.assets.{AssetImpl, Asset}
import be.angelcorp.omicronai.ai.{ActionExecutor, AI}
import be.angelcorp.omicronai.ai.actions.Action

class Soldier( val ai: AI, val aiExec: ActionExecutor, obj: IGameObject ) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  logger.debug(s"Promoted asset $name to a soldier")

  val asset: Asset = TypedActor(context).typedActorOf(TypedProps(classOf[AssetImpl], new AssetImpl(ai, obj)), name="Asset")

  var nextAction: Option[Action] = None

  def act = {
    case NewTurn( turn ) =>
      nextAction match {
        case Some( action ) => doAction( action )
        case None => context.parent ! ActionRequest()
      }

    case ActionUpdate( newAction ) =>
      nextAction = Some( newAction )
      doAction( newAction )

    case Sleep() =>
      context.parent ! Ready()

    case GetAsset() =>
      logger.debug(s"Soldier $name was asked for its asset by $sender")
      sender ! asset

    case ListMetadata() =>
      sender ! Nil

    case msg =>
      logger.info( s"Asset received an unknown message: $msg" )
  }

  def doAction( action: Action ) = {
    action.execute( aiExec ) match {
      // Action successful, ask for the next action to take
      case None =>
        context.parent ! ActionRequest()
      // Action failed, but can be resumed next turn
      case Some( ex ) if ex.isTurnConstrained =>
        nextAction = action.recover(ex)
        context.parent ! Ready()
      case Some( ex ) =>
        context.parent ! ActionFailed(action, ex)
    }
  }

}

sealed abstract class SoldierMessage
case class GetAsset()                           extends SoldierMessage
