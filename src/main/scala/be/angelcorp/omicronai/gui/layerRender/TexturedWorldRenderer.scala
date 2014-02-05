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
import be.angelcorp.omicronai.world.{WorldState, LocationStates, KnownState, GhostState}

class TexturedWorldRenderer( world: ActorRef ) extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit def timeout: Timeout = 50 milliseconds;

  override def render(g: Graphics, view: ViewPort) = {
    Textures.get("terrain.grass") foreach {
      img =>
        val futureStates = ask(world, LocationStates(view.tilesInView.toList)).mapTo[Seq[WorldState]]
        try {
          val locations = Await.result( futureStates, Duration(50, TimeUnit.MILLISECONDS) ).map {
            case KnownState(loc, _, _) => Some(loc)
            case GhostState(loc, _, _) => Some(loc)
            case _ => None
          }
          img.startUse()
          for (optionalLocation <- locations; loc <- optionalLocation) {
            val (x, y) = Canvas.center(loc)
            img.drawEmbedded(x - img.getWidth/2, y - img.getHeight/2)
          }
          img.endUse()
        } catch {
          case e: TimeoutException => logger.warn(s"World layer could not get the state of all the tiles in view within 50ms.")
        }
    }
  }

  override val toString = "Textured terrain"

}
