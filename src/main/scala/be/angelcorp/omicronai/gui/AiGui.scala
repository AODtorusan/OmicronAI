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
import de.lessvoid.nifty.Nifty
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.{LevelType, Game}
import be.angelcorp.omicronai._
import be.angelcorp.omicronai.agents._
import scala.collection.mutable.ListBuffer
import be.angelcorp.omicronai.gui.layerRender._
import akka.util.Timeout
import scala.concurrent.Await
import de.lessvoid.nifty.slick2d.input.{NiftySlickInputSystem, SlickSlickInputSystem}
import de.lessvoid.nifty.controls.{TreeItem, TreeBox, ListBox}
import org.newdawn.slick.geom.Polygon
import scala.Some
import be.angelcorp.omicronai.goals.SquareArea
import javax.swing.SwingUtilities
import de.lessvoid.nifty.controls.treebox.builder.TreeBoxBuilder
import scala.Some
import be.angelcorp.omicronai.agents.Self
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.agents.ListMembers
import scala.Some
import be.angelcorp.omicronai.agents.Self
import be.angelcorp.omicronai.agents.ValidateAction
import be.angelcorp.omicronai.SupervisorMessage

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

    nifty.addScreen( screens.Introduction.name, screens.Introduction.screen(nifty, this) )
    nifty.addScreen( screens.MainMenu.name,     screens.MainMenu.screen(nifty, this)     )

    loadNiftyContent( nifty )

    nifty.gotoScreen( screens.Introduction.name )
  }

  def loadNiftyContent( nifty: Nifty ) {
    val mainMenu = nifty.getScreen(screens.MainMenu.name)

    val layerList = mainMenu.findNiftyControl("activeLayerList", classOf[ListBox[LevelType]])
    LevelType.values().foreach( layerList.addItem )
    layerList.selectItem( LevelType.GROUND )

    val lb = mainMenu.findNiftyControl("layerList", classOf[ListBox[LayerRenderer]])
    lb.addItem( new GridRenderer(pike, Color.white) )
    lb.addItem( new FieldOfView(pike, Color.white)     )
    lb.addItem( new ObjectLayer(pike, go => go.getPlayer == pike, "Friendly units", Color.green, Color.transparent ) )
    lb.addItem( new ObjectLayer(pike, go => go.getPlayer != pike, "Enemy units",    Color.red,   Color.transparent ) )
    lb.addItem( new LayerRenderer {
      val logger = Logger( LoggerFactory.getLogger( getClass ) )
      val assets = mutable.Map[ActorRef, Asset]()
      implicit val timeout: Timeout = 5 seconds;
      def selected = {
        val messagesList = nifty.getScreen(screens.MainMenu.name).findNiftyControl("messageList", classOf[ListBox[SupervisorMessage]])
        messagesList.getSelection.asScala.headOption.flatMap( _.message match {
          case m: ValidateAction => Some(m)
          case _ => None
        } )
      }
      def render(g: Graphics, view: ViewPort) {
        selected match {
          case Some(a) =>
            logger.warn("No way to retrieve metadata")
            //for (m <- a.action.metadata; l <- m.layers) l._2.render(g, view)
          case None =>
        }
      }
      override def toString: String = "Planned action preview"
    } )

    lb.selectItemByIndex(0)
    renderLayers.appendAll(lb.getSelection.asScala)

    val unitTree = mainMenu.findNiftyControl("unitTree", classOf[TreeBox[ActorRef]])

    def buildTree(a: ActorRef): TreeItem[ActorRef] = {
      val i = new TreeItem[ActorRef](a)
      implicit val timeout: Timeout = 5 seconds;
      val children = Await.result( ask(a, ListMembers()), timeout.duration ).asInstanceOf[Iterable[ActorRef]]
      children.foreach( c => i.addTreeItem( buildTree(c) ) )
      i
    }
    val root = new TreeItem[ActorRef](ActorRef.noSender)
    root.addTreeItem(buildTree(pike.admiralRef))
    unitTree.setTree( root )
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

    g.resetTransform()
  }

}

object AiGui extends App {
  SLF4JBridgeHandler.install()

  val app = new AppGameContainer(new AiGui())
  app.setTargetFrameRate(24)
  app.setDisplayMode(800, 550, false)
  app.start()
}
