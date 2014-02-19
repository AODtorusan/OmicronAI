package be.angelcorp.omicron.base.ai.actions

import scala.concurrent.ExecutionContext
import akka.actor.ActorRef
import org.newdawn.slick.Color
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicron.base.{Direction, Location}
import be.angelcorp.omicron.base.algorithms.MovementPathfinder
import be.angelcorp.omicron.base.bridge.Asset
import be.angelcorp.omicron.base.gui.layerRender.{LayerRenderer, PolyLineRenderer}
import be.angelcorp.omicron.base.gui.slick.DrawStyle

case class MoveAction( asset: Asset, destination: Location, world: ActorRef ) extends Action {
  lazy val solution = new MovementPathfinder(destination, asset, world).findPath(asset.location)

  def preview  =
    new PolyLineRenderer( solution._1.path.reverse.dropWhile( _ != asset.location ), DrawStyle(Color.yellow, 3.0f) )

  override def execute(ai: ActionExecutor)(implicit context: ExecutionContext = ai.executionContext) =
    wasSuccess( ai.move(asset, solution._1.path.reverse) )

  override def recover(failure: ActionExecutionException) =
    if ( asset.location == destination ) None else Some(MoveAction( asset, destination, world ) )

}

case class MoveInRangeAction( asset: Asset, destination: Location, range: Int, world: ActorRef ) extends Action {
  lazy val path = {
    val completePath = new MovementPathfinder(destination, asset, world).findPath(asset.location)._1.path.reverse
    completePath.takeWhile( step => (step δ destination) > (range - 1)  ) // -1 because to step into the required range
  }

  lazy val preview = new PolyLineRenderer(path, Color.yellow)

  override def execute(ai: ActionExecutor)(implicit context: ExecutionContext = ai.executionContext) =
    wasSuccess( ai.move(asset, path) )

  override def recover(failure: ActionExecutionException) =
    if ( asset.location == destination ) None else Some(MoveAction( asset, destination, world ) )

}

case class MoveTowards( asset: Asset, direction: Direction ) extends Action {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  lazy val preview  = {
    val location = asset.location
    location.neighbour(direction) match {
      case Some(target) =>
        new PolyLineRenderer( Seq(location, target), DrawStyle(Color.yellow, 2.0f) )
      case None =>
        logger.warn(s"Cannot create layerrenderer to move from $location to the $direction. End position does not exist!")
        LayerRenderer()
    }
  }

  override def execute(ai: ActionExecutor)(implicit context: ExecutionContext = ai.executionContext) =
    wasSuccess( ai.move(asset, direction) )

  override def recover(failure: ActionExecutionException) =
    Some(this)

}
