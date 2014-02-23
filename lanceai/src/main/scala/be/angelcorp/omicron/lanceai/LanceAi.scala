package be.angelcorp.omicron.lanceai

import scala.Some
import scala.collection.mutable
import akka.actor.{ActorSystem, Props}
import de.lessvoid.nifty.Nifty
import org.slf4j.LoggerFactory
import org.newdawn.slick.Graphics
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.{Security, Game, Color, PlayerKey}
import com.lyndir.omicron.api.model.Color.Template._
import be.angelcorp.omicron.base.HexTile
import be.angelcorp.omicron.base.ai.{AIBuilder, AI}
import be.angelcorp.omicron.base.configuration.Configuration._
import be.angelcorp.omicron.base.gui.{Canvas, GuiInterface, AiGuiOverlay}
import be.angelcorp.omicron.base.gui.input.{MouseClicked, InputHandler}
import be.angelcorp.omicron.base.gui.layerRender.{GridRenderer, LayerRenderer}
import be.angelcorp.omicron.base.world.SubWorld
import be.angelcorp.omicron.lanceai.gui.LanceUserInterface

class LanceAi( val actorSystem: ActorSystem, playerId: Int, key: PlayerKey, name: String, color: Color ) extends AI( playerId, key, name, color, color ) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  Security.authenticate(this, key)

  implicit val context = actorSystem.dispatcher
  val world = actorSystem.actorOf(Props.empty)

  def buildGuiInterface(gui: AiGuiOverlay, nifty: Nifty): GuiInterface = new GuiInterface {
    nifty.addScreen( LanceUserInterface.name, LanceUserInterface.screen(nifty, gui) )

    nifty.gotoScreen( LanceUserInterface.name )

    var fromTile: Option[HexTile] = None
    var toTile: Option[HexTile] = None

    actorSystem.actorOf( Props(
      new InputHandler {
        override def receive = {
          case MouseClicked(x, y, 0, 1) =>
            val tile = gui.view.pixelToTile(x, y)
            fromTile = toTile
            toTile = Some(tile)
        }
      }
    ) )

    val activeLayers = mutable.ListBuffer[ LayerRenderer ]()
    activeLayers += new GridRenderer(LanceAi.this)
    activeLayers += new LayerRenderer {
      val  sz = gui.game.getLevelSize
      override def prepareRender(subWorld: SubWorld, layer: Int) {}
      override def render(g: Graphics) {
        for (from <- fromTile; to <- toTile) {
          g.setColor( org.newdawn.slick.Color.magenta )
          g.drawLine( from.centerXY._1* Canvas.scale, from.centerXY._2* Canvas.scale, to.centerXY._1* Canvas.scale, to.centerXY._2 * Canvas.scale)
        }
      }
    }

  }

}

object LanceAi extends AIBuilder {

  def apply( actorSystem: ActorSystem, key: PlayerKey, builder: Game.Builder) =
    new LanceAi( actorSystem, builder.nextPlayerID, key, config.ai.name, RED.get )

  def apply( actorSystem: ActorSystem, key: PlayerKey, name: String, color: Color, builder: Game.Builder) =
    new LanceAi( actorSystem, builder.nextPlayerID, key, name, color )

}
