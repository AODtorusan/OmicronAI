package be.angelcorp.omicronai.gui

import collection.JavaConverters._
import collection.mutable
import akka.pattern.ask
import akka.actor.{ActorRef, Props}
import scala.concurrent.duration._
import org.newdawn.slick.{Color, Graphics, GameContainer, AppGameContainer}
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import de.lessvoid.nifty.loaderv2.types.NiftyType
import de.lessvoid.nifty.slick2d.NiftyOverlayGame
import de.lessvoid.nifty.{NiftyEventAnnotationProcessor, Nifty}
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.{LevelType, Game}
import be.angelcorp.omicronai._
import be.angelcorp.omicronai.agents._
import scala.collection.mutable.ListBuffer
import be.angelcorp.omicronai.gui.layerRender._
import akka.util.Timeout
import scala.concurrent.Await
import de.lessvoid.nifty.slick2d.input.{SlickSlickInputSystem}
import de.lessvoid.nifty.controls.{TreeItem, TreeBox, ListBox}
import javax.swing.SwingUtilities
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.agents.ListMembers
import scala.Some
import be.angelcorp.omicronai.agents.Self
import be.angelcorp.omicronai.agents.ValidateAction
import be.angelcorp.omicronai.SupervisorMessage
import be.angelcorp.omicronai.gui.screens.ui._
import be.angelcorp.omicronai.gui.screens.ui
import scala.Some
import be.angelcorp.omicronai.agents.Self

class AiGui extends NiftyOverlayGame {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  logger.info("Building new game")
  val builder = Game.builder
  val pike = new PikeAi( (player, system) => {
    system.actorOf(Props(new Admiral(player)), name = "AdmiralPike")
  }, builder )
  builder.getPlayers.add( pike )
  val game = builder.build

  val closeRequested = false
  val getTitle = "PikeAi gui"

  val supervisorRef = pike.actorSystem.actorOf( Props( new GuiSupervisor(pike.admiralRef, pike) ), name="AiSupervisor" )
  var supervisor = {
    implicit val timeout: Timeout = 5 seconds;
    Await.result( supervisorRef ? Self(), timeout.duration ).asInstanceOf[GuiSupervisor]
  }
  AiSupervisor.supervisor = Some(supervisorRef)

  def initGameAndGUI(container: GameContainer) {
    this.container = container
    initNifty(container, new SlickSlickInputSystem( new AiGuiInput(this) ) )
    SwingUtilities.invokeLater( new Runnable {
      def run() {
        Thread.sleep(1000)
        game.getController.start()
      }
    } )
  }

  def prepareNifty(nifty: Nifty) {
    val niftyType = new NiftyType()

    // This stuff is a workaround to have nifty styles and default controls available in the builder
    val niftyLoader = nifty.getLoader()
    niftyLoader.loadStyleFile(  "nifty-styles.nxs",   "nifty-default-styles.xml",   niftyType, nifty)
    niftyLoader.loadControlFile("nifty-controls.nxs", "nifty-default-controls.xml", niftyType)
    niftyType.create(nifty, nifty.getTimeProvider())

    nifty.addScreen( screens.Introduction.name,     screens.Introduction.screen(nifty, this)     )
    nifty.addScreen( screens.ui.UserInterface.name, screens.ui.UserInterface.screen(nifty, this) )

    loadNiftyControllers( nifty )

    nifty.gotoScreen( screens.Introduction.name )
  }


  var controllers: List[GuiController] = Nil

  def loadNiftyControllers( nifty: Nifty ) {
    val lc  = new LayerController(this, nifty)
    val utc = new UnitTreeController(this, nifty)
    val uic = new UserInterfaceController(this, nifty, utc)
    val mtc = new MessageTabController(this, nifty, utc)
    val ptc = new ProbeTabController(this, nifty, utc)

    controllers = List(lc, utc, uic, mtc, ptc)
    for (controller <- controllers) {
      NiftyEventAnnotationProcessor.process( controller )
      controller.populate()
    }
  }

  def updateUI() {
    controllers.foreach( _.updateUI() )
  }

  def updateGame(container: GameContainer, delta: Int) {}

  var container: GameContainer = null
  lazy val view = new ViewPort(this)
  val renderLayers = ListBuffer[LayerRenderer]()
  var hoverTile: Option[HexTile] = None

  def renderGame(container: GameContainer, g: Graphics) {
    g.clear()
    g.scale( view.scale, view.scale)
    g.translate( view.offset._1, view.offset._2)

    if (view.changed) {
      renderLayers.par.foreach( _.update(view) )
      view.unsetChanged()
    }

    renderLayers.foreach( _.render(g, view) )

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
