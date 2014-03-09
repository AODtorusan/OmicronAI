package be.angelcorp.omicron.base.ai.actions

import scala.Some
import scala.util.Failure
import scala.concurrent._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import akka.util.Timeout
import akka.actor.ActorRef
import akka.pattern.ask
import org.slf4j.LoggerFactory
import org.newdawn.slick.{Color, Graphics}
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.{UnitTypes, UnitType, Game}
import be.angelcorp.omicron.base.{HexTile, Location}
import be.angelcorp.omicron.base.bridge._
import be.angelcorp.omicron.base.configuration.Configuration.config
import be.angelcorp.omicron.base.gui.Canvas
import be.angelcorp.omicron.base.gui.layerRender.{PolyLineRenderer, LayerRenderer}
import be.angelcorp.omicron.base.gui.slick.DrawStyle
import be.angelcorp.omicron.base.world.{KnownState, WorldState, LocationState, SubWorld}

case class ConstructionStartAction( builder: Asset, destination: Location, constructedType: UnitType, world: ActorRef )(implicit val game: Game) extends Action {
  lazy val logger = Logger( LoggerFactory.getLogger( getClass ) )

  lazy val preview = new LayerRenderer {
    var t = System.currentTimeMillis()
    var isFlashing = true
    override def prepareRender(subWorld: SubWorld, layer: Int) {}
    override def render(g: Graphics) {
      if (System.currentTimeMillis() - t > 500) {
        t = System.currentTimeMillis()
        isFlashing = !isFlashing
      }
      val img  = config.gui.unitSet.spriteFor( UnitTypes.CONSTRUCTION ).unit.image
      val tile = HexTile(destination)
      val center = Canvas.center(tile)
      if (isFlashing)
        img.drawFlash( center._1 - img.getWidth/2, center._2 - img.getHeight/2 )
      else
        img.draw( center._1 - img.getWidth/2, center._2 - img.getHeight/2 )
      Canvas.line( g, builder.location.get, tile, DrawStyle(Color.blue, 2) )
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

case class ConstructionAssistAction( builder: Asset, destination: Location, world: ActorRef)(implicit val game: Game) extends Action {
  implicit val timeout: Timeout = Duration(1, TimeUnit.MINUTES)
  lazy val preview = new PolyLineRenderer( Seq(builder.location.get, destination), DrawStyle(Color.blue, 2) )

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

