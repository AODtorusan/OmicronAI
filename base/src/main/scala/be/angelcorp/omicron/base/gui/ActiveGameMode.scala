package be.angelcorp.omicron.base.gui

import scala.Some
import scala.concurrent.{ExecutionContext, Await}
import scala.concurrent.duration.Duration
import java.util.concurrent.{TimeoutException, TimeUnit}
import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.lyndir.omicron.api.model.Game
import com.typesafe.scalalogging.slf4j.Logger
import org.bushe.swing.event.{EventTopicSubscriber, EventServiceLocator}
import org.slf4j.LoggerFactory
import org.newdawn.slick._
import org.newdawn.slick.state.StateBasedGame
import org.newdawn.slick.util.FontUtils
import de.lessvoid.nifty.Nifty
import de.lessvoid.nifty.loaderv2.types.NiftyType
import be.angelcorp.omicron.base.ai.AI
import be.angelcorp.omicron.base.gui.input.{AiGuiInput, InputSystem, GameMode}
import be.angelcorp.omicron.base.{Location, HexTile}
import be.angelcorp.omicron.base.world.{SubWorld, GetSubWorld}
import be.angelcorp.omicron.base.gui.slick.DrawStyle
import be.angelcorp.omicron.base.util.GenericEventBus
import be.angelcorp.omicron.base.gui.layerRender.renderEngine.RenderEngine

class ActiveGameMode(val game: Game, val system: ActorSystem, val opengl: ExecutionContext, val ai: AI) extends GameMode {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit val timeout: Timeout = Duration(100, TimeUnit.MILLISECONDS)

  val getTitle = "Omicron AI"
  val id       = 1

  // Akka event bus for all gui related events and messages
  val guiBus = new GenericEventBus
  // Listens to the nifty bus and forwards all messages to the guiBus
  val niftyBusBridge = new EventTopicSubscriber[AnyRef] {
    override def onEvent(topic: String, event: AnyRef): Unit =
      guiBus.publish( event )
  }
  // Input system that handles unhandled nifty gui events (all input actions not relating to the hud)
  val input = new InputSystem( guiBus )
  // Basic viewport input handler
  system.actorOf( Props(classOf[AiGuiInput], this, guiBus), name = "GuiFrameInput" )

  // Interface object that renders the interface that the user sees
  var guiInterface: GuiInterface = null

  // Engine that does the game rendering logic
  lazy val renderer = new RenderEngine

  def initGameAndGUI(container: GameContainer, game: StateBasedGame) {
    this.container = container
    this.container.setShowFPS(false)

    initNifty(container, game, input )
    ai.start()
  }

  override def prepareNifty(nifty: Nifty, game: StateBasedGame) {
    val niftyType = new NiftyType()

    // This stuff is a workaround to have nifty styles and default controls available in the builder
    val niftyLoader = nifty.getLoader
    niftyLoader.loadStyleFile(  "nifty-styles.nxs",   "nifty-default-styles.xml",   niftyType, nifty)
    niftyLoader.loadControlFile("nifty-controls.nxs", "nifty-default-controls.xml", niftyType)
    niftyType.create(nifty, nifty.getTimeProvider)

    val niftyBus = EventServiceLocator.getEventService("NiftyEventBus")
    niftyBus.subscribe(""".*""".r.pattern, niftyBusBridge )

    guiInterface = ai.buildGuiInterface(this, guiBus, nifty)
  }

  var container: GameContainer = null
  lazy val view = new ViewPort(this)
  var hoverTile: Option[HexTile] = None
  def hoverLocation: Option[Location] = hoverTile.map( t => Location(t, view.activeLayer, game.getController.getGame.getLevelSize) )

  override def updateGame(container: GameContainer, game: StateBasedGame, delta: Int) {}

  override def renderGame(container: GameContainer, game: StateBasedGame, g: Graphics) {
    g.clear()
    g.scale( view.scale, view.scale)
    g.translate( view.offset._1, view.offset._2)

    // Inform the layers if the viewport changed
    if (view.changed) {
      renderer.viewChanged(view)
      view.unsetChanged()
    }

    // Prepare all the layers for rendering with the latest world data
    try {
      val subworld = Await.result( (ai.world ? GetSubWorld(view.viewBounds)).mapTo[SubWorld], timeout.duration )
      renderer.prepareRender(subworld, view.activeLayer)
    } catch {
      case e: TimeoutException =>
        logger.warn("Could not update viewport, world data not received within 100ms.")
    }

    // Render all the layers to the screen
    renderer.render(g)

    hoverTile match {
      case Some(loc) =>
        new Canvas( loc ) {
          override def borderStyle = new DrawStyle(Color.orange, 5.0f)
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
