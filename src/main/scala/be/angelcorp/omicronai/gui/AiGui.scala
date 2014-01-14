package be.angelcorp.omicronai.gui

import scala.Some
import scala.collection.mutable.ListBuffer
import akka.actor.Props
import javax.swing.SwingUtilities
import org.newdawn.slick.{Color, Graphics, GameContainer, AppGameContainer}
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import de.lessvoid.nifty.loaderv2.types.NiftyType
import de.lessvoid.nifty.slick2d.NiftyOverlayGame
import de.lessvoid.nifty.Nifty
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.Game
import be.angelcorp.omicronai._
import be.angelcorp.omicronai.ai.pike.PikeAi
import be.angelcorp.omicronai.Settings.settings
import be.angelcorp.omicronai.ai.lance.LanceAi
import be.angelcorp.omicronai.ai.AI
import be.angelcorp.omicronai.ai.pike.agents.Admiral
import be.angelcorp.omicronai.gui.input.{InputSystem, AiGuiInput}

class AiGui extends NiftyOverlayGame {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  logger.info("Building new game")
  val builder = Game.builder

  val ai: AI = settings.ai.engine match {
    case "PikeAI" => new PikeAi( (player, system) => system.actorOf(Props(new Admiral(player)), name = "AdmiralPike"), builder )
    case _ => new LanceAi( builder )
  }

  builder.addPlayer(ai)
  builder.addGameListener(ai.gameListener)
  val game = builder.build
  logger.info("Game build!")

  val getTitle = "PikeAi gui"
  var closeRequested = false
  var guiInterface: GuiInterface = null
  val input = new InputSystem()
  input.inputHandlers += new AiGuiInput(this)

  def initGameAndGUI(container: GameContainer) {
    this.container = container
    initNifty(container, input )
    SwingUtilities.invokeLater( new Runnable {
      def run() {
        Thread.sleep(1000)
        game.getController.setReady()
      }
    } )
  }

  def prepareNifty(nifty: Nifty) {
    val niftyType = new NiftyType()

    // This stuff is a workaround to have nifty styles and default controls available in the builder
    val niftyLoader = nifty.getLoader
    niftyLoader.loadStyleFile(  "nifty-styles.nxs",   "nifty-default-styles.xml",   niftyType, nifty)
    niftyLoader.loadControlFile("nifty-controls.nxs", "nifty-default-controls.xml", niftyType)
    niftyType.create(nifty, nifty.getTimeProvider)

    guiInterface = ai.buildGuiInterface(this, nifty)
  }

  def updateGame(container: GameContainer, delta: Int) {}

  var container: GameContainer = null
  lazy val view = new ViewPort(this)
  var hoverTile: Option[HexTile] = None

  def renderGame(container: GameContainer, g: Graphics) {
    g.clear()
    g.scale( view.scale, view.scale)
    g.translate( view.offset._1, view.offset._2)

    if (view.changed) {
      guiInterface.activeLayers.par.foreach( _.update(view) )
      view.unsetChanged()
    }

    guiInterface.activeLayers.foreach( _.render(g, view) )

    hoverTile match {
      case Some(loc) =>
        new GuiTile( loc ) {
          override def borderStyle: DrawStyle = new DrawStyle(Color.orange, 5.0f)
          override def fillColor: Color = Color.transparent
        }.render(g)
      case _ =>
    }

    val statusStrings = ListBuffer[String]( view.toString )
    if (hoverTile.isDefined) statusStrings.append( hoverTile.get.toString )

    var y = 30f
    g.resetTransform()
    g.setColor( new Color(0f, 0f, 0f, 0.6f))
    g.fillRect(5, 5, container.getWidth - 10f , 30f + statusStrings.size * 20f )
    g.setColor(Color.white)
    for ( str <- statusStrings ) {
      g.getFont.drawString(10, y, str)
      y = y + 20f
    }
  }

}

object AiGui extends App {
  SLF4JBridgeHandler.install()

  val app = new AppGameContainer(new AiGui())
  app.setTargetFrameRate(24)
  app.setDisplayMode(1000, 550, false)
  app.start()
}
