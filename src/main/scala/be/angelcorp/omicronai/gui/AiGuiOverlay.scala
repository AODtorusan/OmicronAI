package be.angelcorp.omicronai.gui

import com.lyndir.omicron.api.model.Game
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import org.newdawn.slick.{Color, Graphics, GameContainer}
import org.newdawn.slick.state.StateBasedGame
import org.newdawn.slick.util.FontUtils
import de.lessvoid.nifty.Nifty
import de.lessvoid.nifty.loaderv2.types.NiftyType
import be.angelcorp.omicronai.{Location, HexTile}
import be.angelcorp.omicronai.ai.AI
import be.angelcorp.omicronai.gui.input.{AiGuiInput, InputSystem, GameOverlay}
import be.angelcorp.omicronai.gui.slick.DrawStyle

class AiGuiOverlay(val game: Game, val ai: AI) extends GameOverlay {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val getTitle = "PikeAi gui"
  val id       = 1

  var guiInterface: GuiInterface = null
  val input = new InputSystem()
  input.inputHandlers += new AiGuiInput(this)

  def initGameAndGUI(container: GameContainer, game: StateBasedGame) {
    this.container = container
    initNifty(container, game, input )
    this.container.setShowFPS(false)
    ai.start()
  }

  override def prepareNifty(nifty: Nifty, game: StateBasedGame) {
    val niftyType = new NiftyType()

    // This stuff is a workaround to have nifty styles and default controls available in the builder
    val niftyLoader = nifty.getLoader
    niftyLoader.loadStyleFile(  "nifty-styles.nxs",   "nifty-default-styles.xml",   niftyType, nifty)
    niftyLoader.loadControlFile("nifty-controls.nxs", "nifty-default-controls.xml", niftyType)
    niftyType.create(nifty, nifty.getTimeProvider)

    guiInterface = ai.buildGuiInterface(this, nifty)
  }

  override def updateGame(container: GameContainer, game: StateBasedGame, delta: Int) {}

  var container: GameContainer = null
  lazy val view = new ViewPort(this)
  var hoverTile: Option[HexTile] = None
  def hoverLocation: Option[Location] = hoverTile.map( t => Location(t, view.activeLayer, game.getController.getGame.getLevelSize) )

  override def renderGame(container: GameContainer, game: StateBasedGame, g: Graphics) {
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
        new Canvas( loc ) {
          override def borderStyle: DrawStyle = new DrawStyle(Color.orange, 5.0f)
          override def fillColor: Color = Color.transparent
        }.render(g)
      case _ =>
    }

    val statusStrings = Array(
      container.getFPS + " FPS",
      view.toString,
      "Cursor: " + hoverTile.getOrElse("").toString
    )

    var y = 10
    g.resetTransform()
    g.setColor( new Color(0f, 0f, 0f, 0.6f))
    g.fillRect(container.getWidth/2, 5, container.getWidth/2-5, 10f + statusStrings.size * 20f )
    g.setColor(Color.white)
    for ( str <- statusStrings ) {
      FontUtils.drawRight(g.getFont, str, container.getWidth/2 + 5, y, container.getWidth/2 - 15)
      y = y + 20
    }
  }

}
