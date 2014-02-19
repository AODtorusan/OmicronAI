package be.angelcorp.omicron.base.gui.layerRender

import akka.actor.ActorRef
import org.newdawn.slick.{Graphics, Color}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.util.Maybe.Presence
import be.angelcorp.omicron.base.HexTile
import be.angelcorp.omicron.base.gui.Canvas
import be.angelcorp.omicron.base.world.{GhostState, KnownState, SubWorld}

class FieldOfView(world: ActorRef, knownColor: Color = Color.transparent, ghostColor: Color = new Color(0, 0, 0, 125), unknownColor: Color = Color.black) extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  var visible = Map[Presence, Seq[HexTile]]()

  override def prepareRender(subWorld: SubWorld, layer: Int) {
    visible = subWorld.states(layer).groupBy( {
        case (_, s: KnownState) => Presence.PRESENT
        case (_, s: GhostState) => Presence.UNKNOWN
        case  _                 => Presence.ABSENT
    } ).mapValues( _.map( ls => HexTile(ls._1) ).toSeq )
  }

  def render(g: Graphics) {
    Canvas.render(g, visible.getOrElse(Presence.PRESENT, Nil), Color.transparent, knownColor)
    Canvas.render(g, visible.getOrElse(Presence.UNKNOWN, Nil), Color.transparent, ghostColor)
//    Canvas.render(g, visible.getOrElse(Presence.ABSENT,  Nil), Color.transparent, unknownColor)
  }

  override def toString: String = "Field of view"
}
