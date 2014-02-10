package be.angelcorp.omicronai.ai.actions

import akka.pattern.ask
import org.slf4j.LoggerFactory
import org.newdawn.slick.{Color, Graphics}
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.{PublicModuleType, UnitTypes, UnitType}
import com.lyndir.omicron.api.model.IConstructorModule.{OutOfRangeException, IncompatibleLevelException, InaccessibleException}
import be.angelcorp.omicronai.gui.layerRender.{PolyLineRenderer, LayerRenderer}
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.{HexTile, Location}
import be.angelcorp.omicronai.gui.{Canvas, ViewPort}
import be.angelcorp.omicronai.gui.textures.MapIcons
import be.angelcorp.omicronai.gui.slick.DrawStyle
import be.angelcorp.omicronai.ai._
import scala.util.{Failure, Success}
import be.angelcorp.omicronai.world.{KnownState, WorldState, LocationState}
import scala.concurrent._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import be.angelcorp.omicronai.ai.TimedOut
import scala.util.Failure
import scala.Some
import be.angelcorp.omicronai.world.KnownState
import be.angelcorp.omicronai.ai.InFogOfWar
import be.angelcorp.omicronai.world.LocationState
import akka.util.Timeout
import akka.actor.ActorRef

case class ConstructionStartAction( builder: Asset, destination: Location, constructedType: UnitType, world: ActorRef ) extends Action {
  lazy val logger = Logger( LoggerFactory.getLogger( getClass ) )

  lazy val preview = new LayerRenderer {
    var t = System.currentTimeMillis()
    var isFlashing = true
    override def render(g: Graphics, view: ViewPort) {
      if (System.currentTimeMillis() - t > 500) {
        t = System.currentTimeMillis()
        isFlashing = !isFlashing
      }
      val img  = MapIcons.getIcon( UnitTypes.CONSTRUCTION )
      val tile = HexTile(destination)
      val center = Canvas.center(tile)
      if (isFlashing)
        img.drawFlash( center._1 - img.getWidth/2, center._2 - img.getHeight/2 )
      else
        img.draw( center._1 - img.getWidth/2, center._2 - img.getHeight/2 )
      Canvas.line( g, builder.location, tile, DrawStyle(Color.blue, 2) )
    }
  }

  override def execute(ai: ActionExecutor)(implicit context: ExecutionContext = ai.executionContext) = wasSuccess( {
    for( site  <- ai.constructionStart(builder, constructedType, destination)) yield
      site.map( site => ai.constructionAssist(builder, site) )
  } )

  override def recover(failure: ActionExecutionException) = failure match {
    case TooFar(_, _, _, _) => Some( SequencedAction(
      Seq( MoveInRangeAction(builder, destination, 1, world), this )
    ) )
    case _ => Some(this)
  }

}

case class ConstructionAssistAction( builder: Asset, destination: Location, world: ActorRef) extends Action {
  implicit val timeout: Timeout = Duration(1, TimeUnit.MINUTES)
  lazy val preview = new PolyLineRenderer( Seq(builder.location, destination), DrawStyle(Color.blue, 2) )

  override def execute(ai: ActionExecutor)(implicit context: ExecutionContext = ai.executionContext) = wasSuccess(
    ask(ai.world, LocationState(destination)).mapTo[WorldState].flatMap {
      case KnownState(_, Some(obj), _) => ai.constructionAssist(builder, obj)
      case _ => Future.successful(Failure(InFogOfWar(s"Cannot access the construction site to assist (at $destination)")))
    }
  )

  override def recover(failure: ActionExecutionException) = failure match {
    case TooFar(_, _, _, _) => Some( SequencedAction(
        Seq( MoveInRangeAction(builder, destination, 1, world), this )
      ) )
    case _ => Some(this)
  }

}

