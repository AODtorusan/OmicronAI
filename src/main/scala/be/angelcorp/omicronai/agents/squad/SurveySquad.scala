package be.angelcorp.omicronai.agents.squad

import collection.mutable
import com.lyndir.omicron.api.model.{Tile, GameObject, Player}
import com.typesafe.scalalogging.slf4j.Logger
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.{ActorRef, Props}
import org.slf4j.LoggerFactory
import scala.concurrent.Await
import scala.concurrent.duration._
import be.angelcorp.omicronai.agents._
import be.angelcorp.omicronai.algorithms.MovementPathfinder
import be.angelcorp.omicronai.Conversions._
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.Settings.settings
import be.angelcorp.omicronai.Location.location2tile
import be.angelcorp.omicronai.{RegionOfInterest, Namer}

class SurveySquad(val owner: Player,
                  val name: String,
                  val cartographer: ActorRef ) extends Squad {
  import context.dispatcher // For akka's ExecutionContext
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit def timeout: Timeout = settings.ai.messageTimeout seconds;
  logger.debug(s"Created a new survey squad: $name")

  val namer = new Namer[GameObject]( _.getTypeName )

  val actions = mutable.ListBuffer[(ActorRef, Action)]()
  var replan  = true

  var roi: Option[RegionOfInterest] = None

  def act = {
    case AddMember( unit ) =>
      logger.debug(s"$name was asked to absorb a new member: $unit")
      val newName = namer.nameFor(unit)
      context.actorOf(Props(new Soldier(owner, newName, new Asset(owner, unit) )), name=newName )

    case NewSurveyRoi( newRoi ) =>
      roi    = Some(newRoi)
      replan = true

    case SubmitActions()   =>
      logger.debug(s"$name was asked to submit all actions for this turn for validation by its parent")
      planActions()
      sender ! nextViableAction

    case ActionSuccess(action, updates) =>
      actions.indexWhere( _._2 == action ) match {
        case -1 => replan = true // Action not found, so it was not in the original planning. Rework the planning
        case i  => actions.remove(i)
      }
      updatePlanningFor(updates)
      context.parent ! nextViableAction

    case any =>
      logger.debug(s"Invalid message received: $any")
  }

  def updatePlanningFor( updates: Iterator[Any] ) = for (update <- updates)  update match {
    case UpdateLocation(l) =>
      implicit val game = owner.getController.getGameController.getGame
      val tile: Tile = location2tile(l)
      toOption(tile.getContents) match {
        case Some( content ) if content.getPlayer != owner => replan = true
        case _ =>
      }
      cartographer ! UpdateLocation(l)
    case _ =>
  }

  def nextViableAction = {
    val canDoAction = mutable.Map[ActorRef, Boolean]()
    actions.find( entry => {
      val unit   = entry._1
      val action = entry._2
      canDoAction.getOrElseUpdate( unit, {
        val simulationSuccess =  Await.result(unit ? SimulateAction( action ), timeout.duration).asInstanceOf[ActionResult]
        simulationSuccess match {
          case ActionSuccess(a, _) => true
          case _ => false
        }
      } )
    } ) match {
      case Some(action) => ValidateAction(action._2, action._1)
      case None => Ready()
    }
  }

  def planActions() = if (replan || actions.isEmpty) {
    // Remove all old actions
    actions.clear()

    roi match {
      case Some(region) =>
        val moveIntoRoi = for (child <- context.children) yield {
          val moveActions = for {
            asset <- (child ? GetAsset()).mapTo[Asset]
            soldier <- (child ? Self()).mapTo[Soldier]
          } yield {
            if (region inArea asset.location) Nil
            else {
              val solution = new MovementPathfinder(region.center, asset).findPath(asset.location)
              solution._1.path.filterNot(region.inArea).reverse
            }.map(l => (child, MoveTo(l)))
          }
          Await.result(moveActions, timeout.duration)
        }
        actions.appendAll(moveIntoRoi.flatten)

      case None =>
    }
  }

}

case class NewSurveyRoi( roi: RegionOfInterest )