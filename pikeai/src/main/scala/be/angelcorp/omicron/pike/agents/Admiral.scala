package be.angelcorp.omicron.pike.agents

import scala.collection.mutable
import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor.{TypedProps, TypedActor, Props, ActorRef}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import be.angelcorp.omicron.base.ai.AI
import be.angelcorp.omicron.base.ai.actions.{Action, ActionExecutionException, ActionExecutor}
import be.angelcorp.omicron.base.bridge.{NewTurn, PlayerLostObject, PlayerGainedObject}
import be.angelcorp.omicron.base.configuration.Configuration.config
import be.angelcorp.omicron.base.world.World

class Admiral(protected val ai: AI, protected[pike] val key: PlayerKey) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit def timeout: Timeout = config.ai.messageTimeout seconds;

  protected[pike] val world =
    context.actorOf(World(ai, key, ai.getController.getGameController.getGame.getLevelSize), name = "World" )

  protected[pike] val aiExec: ActionExecutor =
    TypedActor(context).typedActorOf(TypedProps(classOf[ActionExecutor], new ActionExecutor {
      override implicit val game = ai.getController.getGameController.getGame
      override implicit def executionContext = context.dispatcher
      override val world     = Admiral.this.world
      override val playerKey = key
      override val player    = ai
    } ), name="Ai_Execution_Context")
  private lazy val aiExecActor = TypedActor(context).getActorRefFor(aiExec)

  protected[pike] val tacticalGeneral =
    context.actorOf(Props(classOf[PikeTactical], ai, key, aiExec), name = "TacticalGeneral"   )

  private val readyUnits = mutable.Set[ActorRef]()

  override def preStart() {
    val events = List( classOf[PlayerGainedObject], classOf[PlayerLostObject], classOf[NewTurn] )
    events.foreach( event => context.system.eventStream.subscribe(self, event) )
  }

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
      readyUnits += aiExecActor

    case Ready() =>
      readyUnits.add( sender )
      logger.debug( s"$name is marking $sender as ready. Waiting for: ${context.children.filterNot(readyUnits.contains)}" )
      if ( context.children.forall( readyUnits.contains ) ) {
        logger.info("All units have reported to be ready, done with this turn.")
        Thread.sleep(1000)
        withSecurity { ai.getController.getGameController.setReady() }
      }

    case NotReady() =>
      readyUnits.remove( sender )
      logger.debug( s"$name is marking $sender as NOT ready. Waiting for: ${context.children.filterNot(readyUnits.contains)}" )

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
/** Mark the sender as ready */
case class NotReady()
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