package be.angelcorp.omicron.base.gui.layerRender

import scala.Some
import akka.actor.ActorRef
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import org.newdawn.slick.Graphics
import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.base.configuration.Configuration.config
import be.angelcorp.omicron.base.gui.Canvas
import be.angelcorp.omicron.base.world.{GhostState, KnownState, SubWorld}
import be.angelcorp.omicron.base.sprites.Sprite

class TexturedWorldRenderer( world: ActorRef ) extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  var locations = Array.ofDim[(Location, Sprite)](0)

  override def prepareRender(subWorld: SubWorld, layer: Int) {
    val set = config.gui.terrainSet
    locations = subWorld.states(layer).map {
      case (loc, s: KnownState) => Some( loc -> set.spriteFor(loc, s.toGhost) )
      case (loc, s: GhostState) => Some( loc -> set.spriteFor(loc, s)         )
      case _ => None
    }.flatten
  }

  override def render(g: Graphics) = {
    for ((loc, sprite) <- locations) {
      val (x, y) = Canvas.center(loc)
      sprite.image.drawCentered(x, y)
    }
  }

  override val toString = "Textured terrain"

}
