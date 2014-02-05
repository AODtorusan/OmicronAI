package be.angelcorp.omicronai.gui.layerRender

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import java.util.concurrent.TimeUnit
import org.newdawn.slick.{Graphics, Color}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.util.Maybe.Presence
import be.angelcorp.omicronai.HexTile
import be.angelcorp.omicronai.gui.{ViewPort, Canvas}
import be.angelcorp.omicronai.world.{WorldState, LocationStates, GhostState, KnownState}

class FieldOfView(world: ActorRef, knownColor: Color = Color.transparent, ghostColor: Color = new Color(0, 0, 0, 125), unknownColor: Color = Color.black) extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit def timeout: Timeout = 50 milliseconds;

  import scala.concurrent.ExecutionContext.Implicits.global

  def render(g: Graphics, view: ViewPort) {
    val viewLocations = view.tilesInView.toSeq
    val futureVisible = for ( states <- ask(world, LocationStates(viewLocations)).mapTo[Seq[WorldState]] ) yield
              states.zip(viewLocations).groupBy( {
                case (s: KnownState, _) => Presence.PRESENT
                case (s: GhostState, _) => Presence.UNKNOWN
                case  _                 => Presence.ABSENT
              } )
    try {
      val visible = Await.result( futureVisible, Duration(50, TimeUnit.MILLISECONDS) )
      Canvas.render(g, visible.getOrElse(Presence.PRESENT, Nil).map( {
        case (_, loc) => HexTile(loc)
      } ), Color.transparent, knownColor)
      Canvas.render(g, visible.getOrElse(Presence.UNKNOWN, Nil).map( {
        case (_, loc) => HexTile(loc)
      } ), Color.transparent, ghostColor)
      Canvas.render(g, visible.getOrElse(Presence.ABSENT,  Nil).map( {
        case (_, loc) => HexTile(loc)
      } ), Color.transparent, unknownColor)
    } catch {
      case e: Throwable => logger.warn(s"Field of view could not get the state of all the tiles in view within 50ms")
    }
  }

  override def toString: String = "Field of view"
}
