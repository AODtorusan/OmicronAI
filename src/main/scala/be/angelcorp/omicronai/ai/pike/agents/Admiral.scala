package be.angelcorp.omicronai.ai.pike.agents

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor.{Props, ActorRef}
import akka.pattern._
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.configuration.Configuration
import Configuration._
import be.angelcorp.omicronai.world.World
import be.angelcorp.omicronai.ai.AI

class Admiral(owner: AI) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit def timeout: Timeout = config.ai.messageTimeout seconds;

  // Joint Chiefs of Staff
  protected[pike] lazy val world             = context.actorOf(World(owner, owner.getController.getGameController.getGame.getLevelSize), name = "World" )
  protected[pike] lazy val tacticalGeneral   = context.actorOf(Props(classOf[PikeTactical], owner, world), name = "TacticalGeneral"   )
  protected[pike] lazy val gameMessageBridge = context.actorOf(Props[GameListenerBridge], name = "GameListenerBridge")

  private val readyUnits = mutable.Set[ActorRef]()
  private val assets = mutable.HashMap[IGameObject, Asset]()

  def messageListener =
    Await.result(gameMessageBridge ? Self(), timeout.duration).asInstanceOf[GameListenerBridge]

  def act = {
    case Self() =>
      sender ! this

    case PlayerGainedObject( player, unit ) =>
      logger.info(s"Ai ${owner.getName} received new unit: $unit")
      require( player == owner )
      val asset = new Asset(owner, unit)
      assets += ((unit, asset))
      tacticalGeneral ! AddMember( asset )

    case PlayerLostObject( player, unit) =>
      if (player == owner) {
        logger.info(s"Lost object: $unit")
      } else {
        logger.info(s"Enemy $player lost object: $unit")
      }

    case NewTurn( currentTurn ) =>
      logger.info(s"Ai ${owner.getName} is starting turn ${currentTurn.getNumber}")
      readyUnits.clear()
      readyUnits += world
      readyUnits += gameMessageBridge
      tacticalGeneral ! NewTurn( currentTurn )

    case Ready() =>
      readyUnits.add( sender )
      logger.debug( s"$name is marking $sender as ready. Waiting for: ${context.children.filterNot(readyUnits.contains)}" )
      if ( context.children.forall( readyUnits.contains ) )
        owner.getController.getGameController.setReady()

    case ListMetadata() =>
      sender ! Nil
  }

  override def unhandled(event: Any) {
    logger.warn(s"$name ignored message: $event")
  }

}

sealed abstract class AdmiralMessage
case class Self()                               extends AdmiralMessage

/** Asks children to submit actions that they will perform */
case class SubmitActions() extends AdmiralMessage
case class Ready() extends AdmiralMessage
/** Reply by a child to a parent to clear an action for execution */
case class ValidateAction( action: Action, unit: ActorRef ) extends AdmiralMessage
/** Answer from a parent that an action may be executed */
case class ExecuteAction( action: Action ) extends AdmiralMessage
/** Answer from a parent that an action may not be executed */
case class RevokeAction( action: Action ) extends AdmiralMessage
/** Answer from a parent that an alternative action should be executed */
case class OverruleAction( oldAction: Action, newAction: Action ) extends AdmiralMessage

/** Ask a unit to simulate an action (check if it can execute an action) */
case class SimulateAction( action: Action )
/** Result of an action */
sealed abstract class ActionResult
case class ActionSuccess( action: Action, updates: Iterator[Any] = Iterator() ) extends ActionResult
case class ActionFailed(  action: Action, message: String, reason: FailureReason = UnknownError() ) extends ActionResult

case class AddMember(  unit: Asset )       extends AdmiralMessage
case class ListMembers()                   extends AdmiralMessage
case class ListMetadata()                  extends AdmiralMessage

abstract class FailureReason
case class MissingModule() extends FailureReason
case class OutOfSpeed()    extends FailureReason
case class UnknownError()  extends FailureReason