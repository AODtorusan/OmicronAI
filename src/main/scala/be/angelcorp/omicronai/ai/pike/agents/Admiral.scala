package be.angelcorp.omicronai.ai.pike.agents

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor.{TypedProps, TypedActor, Props, ActorRef}
import akka.pattern._
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import be.angelcorp.omicronai.assets.{AssetImpl, Asset}
import be.angelcorp.omicronai.configuration.Configuration._
import be.angelcorp.omicronai.world.World
import be.angelcorp.omicronai.ai.{ActionExecutor, ActionExecutionException, AI}
import be.angelcorp.omicronai.ai.actions.Action

class Admiral(protected val ai: AI) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit def timeout: Timeout = config.ai.messageTimeout seconds;

  // Joint Chiefs of Staff
  protected[pike] lazy val world             = context.actorOf(World(ai, ai.getController.getGameController.getGame.getLevelSize), name = "World" )
  protected[pike] lazy val tacticalGeneral   = context.actorOf(Props(classOf[PikeTactical], ai, aiExec), name = "TacticalGeneral"   )
  protected[pike] lazy val gameMessageBridge = context.actorOf(Props[GameListenerBridge], name = "GameListenerBridge")

  protected[pike] lazy val aiExec: ActionExecutor =
    TypedActor(context).typedActorOf(TypedProps(classOf[ActionExecutor], new ActionExecutor {
      override implicit val game = ai.getController.getGameController.getGame
      override val world = Admiral.this.world
    } ), name="Ai_Execution_Context")
  private lazy val aiExecActor = TypedActor(context).getActorRefFor(aiExec)

  private val readyUnits = mutable.Set[ActorRef]()

  def messageListener =
    Await.result(gameMessageBridge ? Self(), timeout.duration).asInstanceOf[GameListenerBridge]

  def act = {
    case Self() =>
      sender ! this

    case PlayerGainedObject( player, unit ) =>
      logger.info(s"Ai ${ai.getName} received new unit: $unit")
      require( player == ai )
      tacticalGeneral ! AddMember( unit )

    case PlayerLostObject( player, unit) =>
      if (player == ai) {
        logger.info(s"Lost object: $unit")
      } else {
        logger.info(s"Enemy $player lost object: $unit")
      }

    case NewTurn( currentTurn ) =>
      logger.info(s"Ai ${ai.getName} is starting turn ${currentTurn.getNumber}")
      readyUnits.clear()
      readyUnits += world
      readyUnits += gameMessageBridge
      readyUnits += aiExecActor
      tacticalGeneral ! NewTurn( currentTurn )

    case Ready() =>
      readyUnits.add( sender )
      logger.debug( s"$name is marking $sender as ready. Waiting for: ${context.children.filterNot(readyUnits.contains)}" )
      if ( context.children.forall( readyUnits.contains ) )
        ai.getController.getGameController.setReady()

    case ListMetadata() =>
      sender ! Nil
  }

  override def unhandled(event: Any) {
    logger.warn(s"$name ignored message: $event")
  }

}

sealed abstract class AdmiralMessage
case class Self()                               extends AdmiralMessage

/** Mark the sender as ready */
case class Ready()
/** Mark the receiver to not perform any more actions until next turn */
case class Sleep()

case class ActionRequest()
case class ActionUpdate( action: Action )
case class ActionFailed( action: Action, ex: ActionExecutionException )

case class AddMember(  unit: IGameObject ) extends AdmiralMessage
case class ListMembers()                   extends AdmiralMessage
case class ListMetadata()                  extends AdmiralMessage

abstract class FailureReason
case class MissingModule() extends FailureReason
case class OutOfSpeed()    extends FailureReason
case class UnknownError()  extends FailureReason