package be.angelcorp.omicronai.agents

import collection.mutable
import akka.actor.{Props, ActorRef}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.controller.{GameController, PlayerController}

class Admiral(owner: Player) extends PlayerController(owner) with Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val name = "Admiral"

  // Joint Chiefs of Staff
  var resourceGeneral: ActorRef = null
  var strategyGeneral: ActorRef = null
  var tacticalGeneral: ActorRef = null

  val readyUnits = mutable.Set[ActorRef]()

  override def preStart {
    //resourceGeneral = context.actorOf(Props[DeafAgent], "resource general")
    //strategyGeneral = context.actorOf(Props[DeafAgent], "strategy general")

    tacticalGeneral = context.actorOf(Props(new PikeTactical(owner)), name = "TacticalGeneral")

    //aiPlayer.getController.iterateObservableObjects(aiPlayer).iterator.foreach( unit => {
    //  tacticalGeneral ! NewUnit( unit )
    //} )
  }

  def act = {
    case Self() =>
      sender ! this

    case NewTurn() =>
      logger.debug(s"Admiral is asking for proposed orders by all units")
      readyUnits.clear()
      tacticalGeneral ! NewTurn()

    case Ready() =>
      readyUnits.add( sender )
      logger.debug( s"$name is marking $sender as ready. Waiting for: ${context.children.filterNot(readyUnits.contains)}" )
      if ( context.children.forall( readyUnits.contains ) )
        getGameController.setReady( owner )

    case ListMetadata() =>
      sender ! Nil

    case event =>
      logger.warn(s"Dude what the hell are you trying to tell me, I don't get this: $event")
  }

  override def onNewTurn(gameController: GameController) {
    logger.info(s"Ai ${owner.getName} is starting turn ${gameController.getGame.getCurrentTurn.getNumber}")
    super.onNewTurn(gameController)

    self ! NewTurn()
  }

  override def addObject(unit: GameObject) {
    logger.info(s"Ai ${owner.getName} received new unit: ${unit}")
    super.addObject(unit)

    tacticalGeneral ! AddMember( unit )
  }
}

sealed abstract class AdmiralMessage
case class Self()                               extends AdmiralMessage
case class NewTurn()                            extends AdmiralMessage

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
case class ActionFailed(  action: Action, message: String, reason: FailureReason = Unknown() ) extends ActionResult

case class AddMember(  unit: GameObject )       extends AdmiralMessage
case class ListMembers()                        extends AdmiralMessage
case class ListMetadata()                       extends AdmiralMessage
case class Name()                               extends AdmiralMessage

abstract class FailureReason
case class MissingModule() extends FailureReason
case class OutOfSpeed()    extends FailureReason
case class Unknown()       extends FailureReason