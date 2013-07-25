package be.angelcorp.omicronai.agents

import scala.concurrent.Await
import scala.concurrent.duration._
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.controller.{GameController, PlayerController}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicronai.actions.Action
import be.angelcorp.omicronai.goals.Goal
import akka.actor.{Actor, Props, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import be.angelcorp.omicronai.Settings.settings
import be.angelcorp.omicronai.AiSupervisor

class Admiral(player: Player) extends PlayerController(player) with Actor {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  // Joint Chiefs of Staff
  var resourceGeneral: ActorRef = null
  var strategyGeneral: ActorRef = null
  var tacticalGeneral: ActorRef = null

  override def preStart {
    //resourceGeneral = context.actorOf(Props[DeafAgent], "resource general")
    //strategyGeneral = context.actorOf(Props[DeafAgent], "strategy general")

    tacticalGeneral = context.actorOf(Props(new PikeTactical(player)), name = "TacticalGeneral")

    //aiPlayer.getController.iterateObservableObjects(aiPlayer).iterator.foreach( unit => {
    //  tacticalGeneral ! NewUnit( unit )
    //} )
  }

  def receive = {
    case Self() =>
      sender ! this

    case NewTurn() =>
      AiSupervisor.supervisor match {
        case Some(supervisor) if settings.ai.supervisor.onNewTurn && sender != supervisor =>
          logger.debug(s"Forwarding new turn command to supervisor")
          supervisor.forward( NewTurn() )
        case _ =>
          logger.debug(s"Admiral is asking for proposed orders by all units")
          tacticalGeneral ! SubmitActions()
      }

    case Name() =>
      logger.trace(s"Admiral was asked for a its name by $sender")
      sender ! "Admiral"

    case ValidateAction(a, s) =>
      AiSupervisor.supervisor match {
        case Some(supervisor) if settings.ai.supervisor.onValidateAction && sender != supervisor =>
          logger.debug(s"Forwarding action validation message to supervisor")
          supervisor.forward( ValidateAction(a, s) )
        case _ =>
          implicit val timeout: Timeout = 5 seconds;
          logger.debug(s"Admiral was asked to validate action $a for unit ${Await.result(s ? Name(), timeout.duration).asInstanceOf[String]}")
          logger.debug(s"No external validator present, so action is accepted.")
          s ! ExecuteAction(a)
      }

    case event =>
      logger.warn(s"Dude what the hell are you trying to tell me, I don't get this: $event")
  }

  override def onNewTurn(gameController: GameController) {
    logger.info(s"Ai ${player.getName} is starting turn ${gameController.getGame.getCurrentTurn.getNumber}")
    super.onNewTurn(gameController)

    self ! NewTurn()
  }

  override def addObject(unit: GameObject) {
    logger.info(s"Ai ${player.getName} received new unit: ${unit}")
    super.addObject(unit)

    tacticalGeneral ! AddMember( unit )
  }
}

sealed abstract class AdmiralMessage
case class Self()                               extends AdmiralMessage
case class NewTurn()                            extends AdmiralMessage

/** Asks children to submit actions that they will perform */
case class SubmitActions() extends AdmiralMessage
/** Reply by a child to a parent to clear an action for execution */
case class ValidateAction( a: Action, soldier: ActorRef ) extends AdmiralMessage
/** Answer from a parent that an action may be executed */
case class ExecuteAction( a: Action ) extends AdmiralMessage
/** Answer from a parent that an action may not be executed */
case class RevokeAction( a: Action ) extends AdmiralMessage
/** Answer from a parent that an alternative action should be executed */
case class OverruleAction( oldAction: Action, newAction: Action ) extends AdmiralMessage

case class AddMember(  unit: GameObject )       extends AdmiralMessage
case class SetGoal(    goal: Goal       )       extends AdmiralMessage
case class ListMembers()                        extends AdmiralMessage
case class Name()                               extends AdmiralMessage
