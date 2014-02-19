package be.angelcorp.omicron.base.gui.layerRender

import scala.Some
import akka.actor.ActorRef
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import org.newdawn.slick.Graphics
import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.base.world.{GhostState, KnownState, SubWorld}
import be.angelcorp.omicron.base.gui.Canvas
import be.angelcorp.omicron.base.gui.textures.Textures

class TexturedWorldRenderer( world: ActorRef ) extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  var locations = Array.ofDim[Location](0)

  override def prepareRender(subWorld: SubWorld, layer: Int) {
    locations = subWorld.states(layer).map {
      case (_, KnownState(loc, _, _)) => Some(loc)
      case (_, GhostState(loc, _, _)) => Some(loc)
      case _ => None
    }.flatten
  }

  override def render(g: Graphics) = {
    Textures.get("terrain.grass") foreach {
      img =>
        img.startUse()
        for (loc <- locations) {
          val (x, y) = Canvas.center(loc)
          img.drawEmbedded(x - img.getWidth/2, y - img.getHeight/2)
        }
        img.endUse()
    }
  }

  override val toString = "Textured terrain"

}
