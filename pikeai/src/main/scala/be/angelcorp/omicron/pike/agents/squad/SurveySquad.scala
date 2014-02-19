package be.angelcorp.omicron.pike.agents.squad

import scala.Some
import scala.collection.mutable
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.Success
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import org.slf4j.LoggerFactory
import org.newdawn.slick.Color
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.{PlayerKey, IGameObject}
import be.angelcorp.omicron.base.configuration.Configuration.config
import be.angelcorp.omicron.base.{Location, RegionOfInterest, Namer}
import be.angelcorp.omicron.base.ai.AI
import be.angelcorp.omicron.base.ai.actions.{SequencedAction, MoveAction, ActionExecutor}
import be.angelcorp.omicron.base.bridge.{Asset, NewTurn}
import be.angelcorp.omicron.base.gui.layerRender.{LayerRenderer, RegionRenderer}
import be.angelcorp.omicron.base.metadata.MetaData
import be.angelcorp.omicron.base.world.{LocationStates, KnownState, WorldState, GhostState}
import be.angelcorp.omicron.pike.agents._
import be.angelcorp.omicron.pike.agents.ActionFailed
import be.angelcorp.omicron.pike.agents.ActionRequest
import be.angelcorp.omicron.pike.agents.AddMember

class SurveySquad(val ai: AI, protected val key: PlayerKey, val aiExec: ActionExecutor ) extends Squad {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  import context.dispatcher

  implicit def timeout: Timeout = config.ai.messageTimeout seconds;
  logger.debug(s"Created a new survey squad: $name")

  val namer = new Namer[IGameObject]( _.getType.getTypeName )
  val readyUnits = mutable.Set[ActorRef]()

  var roi:     Option[RegionOfInterest] = None

  override def preStart(){
    context.system.eventStream.subscribe(self, classOf[NewTurn])
  }

  def act = {
    case AddMember( unit ) =>
      logger.debug(s"$name was asked to absorb a new member: $unit")
      context.actorOf(Props(new Soldier(ai, key, aiExec, unit )), name=namer.nameFor(unit) )

    case NewSurveyRoi( newRoi ) =>
      logger.debug(s"$name is updating the region of interest to $newRoi")
      roi    = Some(newRoi)

    case ActionRequest() =>
      actionFor( sender ) pipeTo sender

    case ActionFailed( action, ex ) =>
      ???

    case ListMetadata() =>
      sender ! Iterable( new MetaData() {
        def title: String = "Survey area"
        def layers: Map[String, LayerRenderer] = {
          val roiLayer = new RegionRenderer(roi.get, Color.transparent, new Color(1f, 1f, 1f, 0.5f))
          Map(
            "Region of interest" -> roiLayer
          )
        }
      } )

    case NewTurn( turn ) =>
      readyUnits.clear()

    case Ready() =>
      readyUnits.add( sender )
      logger.debug( s"$name is marking $sender as ready. Waiting for: ${context.children.filterNot(readyUnits.contains)}" )
      if ( context.children.forall( readyUnits.contains ) )
        context.parent ! Ready()

    case NotReady() =>
      readyUnits.remove( sender )
      logger.debug( s"$name is marking $sender as NOT ready. Waiting for: ${context.children.filterNot(readyUnits.contains)}" )

    case any =>
      logger.debug(s"Invalid message received: $any")
  }

  def actionFor( actor: ActorRef ): Future[Any] = {
    logger.trace(s"$name is creating a new action for $actor")

    roi match {
      case Some(region) =>
        val action = (actor ? GetAsset()).mapTo[Asset].flatMap( asset => {
          val scanRadius =  asset.base.getViewRange

          // The complete scan path
          val completeSpiralPath = region.center.spiral(region.radius, 2*scanRadius+1)

          // Remove tiles where enough information exists

          val remainingSpiralPath = Future.traverse(completeSpiralPath)( location => {
              val tilesInView = location.range( scanRadius )
              val futureConfidence =
                for {states <- (aiExec.world ? LocationStates(tilesInView)).mapTo[ Seq[WorldState] ] } yield
                  for (state <- states) yield state match {
                    case s: KnownState => 1.0
                    case s: GhostState => 0.8
                    case _             => 0.0
                  }
              futureConfidence.map( c => if (c.sum / tilesInView.size < 0.9) Some(location) else None )
            } )

          remainingSpiralPath.map( sp => SequencedAction(
            sp.flatten.map(target => MoveAction(asset, target, aiExec.world) ) )
          )
        } )
        action.map( a => ActionUpdate(a) )
      case None => Future.successful( Sleep() )
    }
  }

  // Returns (center, radius)
  def enclosingCircle() = roi match {
    case Some( region ) if region.tiles.nonEmpty =>
      val points = region.tiles
      val first  = points.head

      var (centerU, centerV) = (0.0, 0.0)
      for (point <- points) {
        centerU = centerU + point.u
        centerV = centerV + point.v
      }
      val center = Location( centerU / points.size.toDouble, centerV / points.size.toDouble, first.h, first.bounds )

      var radius = -1
      for (point <- points)
        radius = math.max(radius, center δ point)

      (center, radius)
    case _ =>
      ( Location(0,0,0,null), -1)
  }

}

case class NewSurveyRoi( roi: RegionOfInterest )
