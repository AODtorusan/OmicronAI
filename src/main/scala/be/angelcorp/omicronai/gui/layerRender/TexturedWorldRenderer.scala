package be.angelcorp.omicronai.gui.layerRender

import scala.Some
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor.{Props, ActorRef}
import akka.pattern._
import akka.actor.ActorRef
import java.util.concurrent.{TimeoutException, TimeUnit}
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import org.newdawn.slick.Graphics
import be.angelcorp.omicronai.gui.{Canvas, ViewPort}
import be.angelcorp.omicronai.gui.textures.Textures
import be.angelcorp.omicronai.world._
import be.angelcorp.omicronai.world.KnownState
import be.angelcorp.omicronai.world.GhostState
import scala.Some
import be.angelcorp.omicronai.world.LocationStates
import be.angelcorp.omicronai.Location

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
