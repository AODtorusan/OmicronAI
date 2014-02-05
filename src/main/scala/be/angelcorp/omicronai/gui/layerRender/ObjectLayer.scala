package be.angelcorp.omicronai.gui.layerRender

import scala.concurrent.{TimeoutException, Await}
import scala.concurrent.duration._
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.newdawn.slick.{Graphics, Color}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.IGameObject
import be.angelcorp.omicronai.gui.{ViewPort, Canvas}
import be.angelcorp.omicronai.HexTile
import be.angelcorp.omicronai.gui.textures.MapIcons
import be.angelcorp.omicronai.world.{WorldState, LocationStates, GhostState, KnownState}
import be.angelcorp.omicronai.gui.slick.DrawStyle

class ObjectLayer(world:  ActorRef,
                  filter: IGameObject => Boolean,
                  name:   String,
                  knownFill: Color  = Color.green,
                  ghostFill: Color  = Color.lightGray,
                  border: DrawStyle = Color.transparent) extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit def timeout: Timeout = 50 milliseconds;

  import scala.concurrent.ExecutionContext.Implicits.global

  def render(g: Graphics, view: ViewPort) {
    val viewLocations = view.tilesInView.toSeq
    // { object, location, isGhost }
    val futureObjects = for ( states <- ask(world, LocationStates(viewLocations)).mapTo[Seq[WorldState]] ) yield
      states.map( {
        case KnownState(loc, optionContent, _) => optionContent match {
          case Some(obj) if filter(obj) => Some((obj, loc, false))
          case _ => None
        }
        case GhostState(loc, optionContent, _) => optionContent match {
          case Some(obj) if filter(obj) => Some((obj, loc, true))
          case _ => None
        }
        case _          => None
      } ).flatten
    try {
      val objects = Await.result( futureObjects, timeout.duration )
      objects.foreach( entry => {
        val tile: HexTile = entry._2
        new Canvas( tile ) {
          override def borderStyle: DrawStyle = border
          override def fillColor: Color       = if (entry._3) ghostFill else knownFill
        }.render(g)
        val (centerX, centerY) = Canvas.center( tile )
        MapIcons.getIcon( entry._1 ).drawCentered( centerX, centerY )
      } )
    } catch {
      case e: TimeoutException => logger.warn(s"Object layer ($name) could not get the state of all the tiles in view within 50ms.")
    }
  }

  override def toString = name
}
