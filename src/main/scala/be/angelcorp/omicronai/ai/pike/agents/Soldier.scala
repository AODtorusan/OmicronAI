package be.angelcorp.omicronai.ai.pike.agents

import scala.Some
import scala.concurrent.Future
import akka.actor.{TypedProps, TypedActor}
import akka.pattern.pipe
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.IGameObject
import be.angelcorp.omicronai.ai.AI
import be.angelcorp.omicronai.ai.actions.{NextTurn, Now, ActionExecutor, Action}
import be.angelcorp.omicronai.bridge._

class Soldier( val ai: AI, val aiExec: ActionExecutor, obj: IGameObject ) extends Agent {
  implicit val exec = context.dispatcher
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  logger.debug(s"Promoted asset $name to a soldier")

  val asset: Asset = TypedActor(context).typedActorOf(TypedProps(classOf[AssetImpl], new AssetImpl(ai, obj)), name="Asset")

  var nextAction: Option[Action] = None

  override def preStart() {
    context.system.eventStream.subscribe(self, classOf[NewTurn])
    context.parent ! ActionRequest()
  }

  def act = {
    case NewTurn( turn ) =>
      nextAction match {
        case Some( action ) => doAction( action ) pipeTo context.parent
        case None => context.parent ! ActionRequest()
      }

    case ActionUpdate( newAction ) =>
      nextAction = Some( newAction )
      doAction( newAction ) pipeTo context.parent

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

  def doAction( action: Action ): Future[Any] = {
    logger.debug(s"$name is executing action $action.")
    action.execute(aiExec).flatMap {
      // Action successful, ask for the next action to take
      case None =>
        logger.debug(s"Action successful for $name.")
        nextAction = None
        Future.successful(ActionRequest())
      // Action failed, but can be resumed next turn
      case Some(ex) if ex.retryHint == Now =>
        logger.debug(s"Action failed for $name, trying recovery now (${ex.getMessage}).")
        action.recover(ex) match {
          case Some(newAction) =>
            doAction(newAction)
          case None =>
            nextAction = None
            Future.successful(Ready())
        }
      case Some(ex) if ex.retryHint == NextTurn =>
        logger.debug(s"Action failed for $name, trying recovery next turn (${ex.getMessage}).")
        nextAction = action.recover(ex)
        Future.successful(Ready())
      case Some(ex) =>
        logger.debug(s"Action failed for $name, not retrying, asking for new orders.", ex)
        Future.successful(ActionFailed(action, ex))
    }
  }

}

sealed abstract class SoldierMessage
case class GetAsset()                           extends SoldierMessage
