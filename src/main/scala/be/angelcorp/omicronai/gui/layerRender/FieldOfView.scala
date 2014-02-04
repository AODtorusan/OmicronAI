package be.angelcorp.omicronai.gui.layerRender

import org.newdawn.slick.{Graphics, Color}
import be.angelcorp.omicronai.gui.{ViewPort, Canvas}
import be.angelcorp.omicronai.world.{GhostState, KnownState, WorldInterface}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import com.lyndir.omicron.api.util.Maybe.Presence
import be.angelcorp.omicronai.HexTile

class FieldOfView(world: WorldInterface, knownColor: Color = Color.transparent, ghostColor: Color = new Color(0, 0, 0, 125), unknownColor: Color = Color.black) extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  import scala.concurrent.ExecutionContext.Implicits.global

  def render(g: Graphics, view: ViewPort) {
    val viewLocations = view.tilesInView.toSeq
    val futureVisible = for ( states <- world statesOf viewLocations) yield
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
