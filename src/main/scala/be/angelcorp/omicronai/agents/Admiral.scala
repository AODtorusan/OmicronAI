package be.angelcorp.omicronai.agents

import collection.mutable
import akka.actor.{Props, ActorRef}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api._

class Admiral(owner: Player) extends GameListener with Agent {
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
        owner.getController.getGameController.setReady()

    case ListMetadata() =>
      sender ! Nil

    case event =>
      logger.warn(s"Dude what the hell are you trying to tell me, I don't get this: $event")
  }

  override def onPlayerReady(readyPlayer: Player) = logger.warn("No action implemented for onPlayerReady")
  override def onNewTurn(currentTurn: Turn) {
    logger.info(s"Ai ${owner.getName} is starting turn ${currentTurn.getNumber}")
    self ! NewTurn()
  }
  override def onBaseDamaged(baseModule: BaseModule, damage: ChangeInt) = logger.warn("No action implemented for onBaseDamaged")
  override def onTileContents(tile: Tile, contents: Change[GameObject]) {
    logger.info(s"Contents of tile $tile changed to: ${contents.getTo}")
  }
  override def onTileResources(tile: Tile, resourceType: ResourceType, resourceQuantity: ChangeInt) = logger.warn("No action implemented for onTileResources")
  override def onPlayerScore(player: Player, score: ChangeInt) = logger.warn("No action implemented for onPlayerScore")
  override def onPlayerGainedObject(player: Player, unit: GameObject) {
    logger.info(s"Ai ${owner.getName} received new unit: $unit")
    require( player == owner )
    tacticalGeneral ! AddMember( unit )
  }
  override def onPlayerLostObject(player: Player, gameObject: GameObject) {
    if (player == owner) {
      logger.info(s"Lost object: $gameObject")
    } else {
      logger.info(s"Enemy $player lost object: $gameObject")
    }
  }
  override def onUnitCaptured(gameObject: GameObject, owner: Change[Player]) = logger.warn("No action implemented for onUnitCaptured")
  override def onUnitMoved(gameObject: GameObject, location: Change[Tile]) {
    logger.warn(s"Object moved: $gameObject to ${location.getTo}")
  }
  override def onUnitDied(gameObject: GameObject) = logger.warn("No action implemented for onUnitDied")
  override def onContainerStockChanged(containerModule: ContainerModule, stock: ChangeInt) = logger.warn("No action implemented for onContainerStockChanged")
  override def onConstructorWorked(constructorModule: ConstructorModule, remainingSpeed: ChangeInt) = logger.warn("No action implemented for onConstructorWorked")
  override def onConstructorTargeted(constructorModule: ConstructorModule, target: Change[GameObject]) = logger.warn("No action implemented for onConstructorTargeted")
  override def onConstructionSiteWorked(constructionSite: ConstructorModule.ConstructionSite, moduleType: ModuleType[_], remainingWork: ChangeInt) = logger.warn("No action implemented for onConstructionSiteWorked")

  override def onMobilityLeveled(mobilityModule: MobilityModule, location: Change[Tile], remainingSpeed: ChangeDbl) = logger.warn("No action implemented for onMobilityLeveled")
  override def onMobilityMoved(mobilityModule: MobilityModule, location: Change[Tile], remainingSpeed: ChangeDbl) = logger.warn("No action implemented for onMobilityMoved")
  override def onWeaponFired(weaponModule: WeaponModule, target: Tile, repeated: ChangeInt, ammunition: ChangeInt) = logger.warn("No action implemented for onWeaponFired")
  override def onGameStarted(game: Game) = logger.warn("No action implemented for onGameStarted")
  override def onGameEnded(game: Game, victoryCondition: VictoryConditionType, victor: Player) = logger.warn("No action implemented for onGameEnded")
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
case class ActionFailed(  action: Action, message: String, reason: FailureReason = UnknownError() ) extends ActionResult

case class AddMember(  unit: GameObject )       extends AdmiralMessage
case class ListMembers()                        extends AdmiralMessage
case class ListMetadata()                       extends AdmiralMessage
case class Name()                               extends AdmiralMessage

abstract class FailureReason
case class MissingModule() extends FailureReason
case class OutOfSpeed()    extends FailureReason
case class UnknownError()       extends FailureReason