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
import be.angelcorp.omicronai.{AiSupervisor, PikeAi}
import be.angelcorp.omicronai.agents.{GetAsset, Self, Admiral}
import scala.collection.mutable.ListBuffer
import be.angelcorp.omicronai.gui.layerRender.{GridRenderer, ObjectLayer, FieldOfView, LayerRenderer}
import akka.util.Timeout
import scala.concurrent.Await
import de.lessvoid.nifty.slick2d.input.{NiftySlickInputSystem, SlickSlickInputSystem}
import de.lessvoid.nifty.controls.ListBox
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.actions.MoveVia
import org.newdawn.slick.geom.Polygon

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
    Await.result( ask(supervisorRef, Self()), timeout.duration ).asInstanceOf[GuiSupervisor]
  }
  AiSupervisor.supervisor = Some(supervisorRef)

  def initGameAndGUI(container: GameContainer) {
    this.container = container
    initNifty(container, new SlickSlickInputSystem( new AiGuiInput(this) ) )
    game.getController.start()
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
        val actionList = mainMenu.findNiftyControl("actionList", classOf[ListBox[WrappedAction]])
        actionList.getSelection.asScala.headOption
      }
      def render(g: Graphics, layer: LevelType) {
        selected match {
          case Some(a) =>
            val asset = assets.getOrElseUpdate(a.actor, Await.result(a.actor ? GetAsset(), timeout.duration).asInstanceOf[Asset])
            a.action match {
              case MoveVia(path) =>
                val poly = new Polygon()
                poly.setClosed(false)

                for ( loc <- asset.location :: path.toList if loc.h == layer.ordinal() ) {
                  val center = GuiTile.center(loc)
                  poly.addPoint( center._1, center._2 )
                }

                new DrawStyle(Color.yellow, 3.0f).applyOnto(g)
                g.draw(poly)
              case action =>
                logger.warn(s"LayerRender does not know how to render action: $action")
            }
          case None =>
        }
      }
      override def toString: String = "Planned action preview"
    } )

    lb.selectItemByIndex(0)
    renderLayers.appendAll(lb.getSelection.asScala)

  }

  def updateGame(container: GameContainer, delta: Int) {}

  var container: GameContainer = null
  var scale  =  0.5f
  var offset = (0.0f, 0.0f)

  var activeLayer  = LevelType.GROUND
  val renderLayers = ListBuffer[LayerRenderer]()

  def renderGame(container: GameContainer, g: Graphics) {
    g.clear()
    g.scale(scale,scale)
    g.translate(offset._1, offset._2)
    renderLayers.foreach( _.render(g, activeLayer) )
    g.resetTransform()
  }

  def scaleBy(delta: Float) {
    scaleTo( scale + delta )
  }

  def scaleTo(newScale: Float) {
    if (newScale > 0) {
      val dx = container.getWidth  * ( 1.0f/newScale - 1.0f/scale)
      val dy = container.getHeight * ( 1.0f/newScale - 1.0f/scale)

      moveBy(dx/2.0f, dy/2.0f)
      scale = newScale
    } else {
      logger.warn(s"Cannot rescale the viewport to a scale <= 0, requested $newScale")
    }
  }

  def moveBy( deltaX: Float, deltaY: Float) {
    offset = (offset._1 + deltaX, offset._2 + deltaY)
  }

  def moveTo( xOffset: Float, yOffset: Float) {
    offset = (xOffset, yOffset)
  }
}

object AiGui extends App {
  SLF4JBridgeHandler.install()

  val app = new AppGameContainer(new AiGui())
  app.setTargetFrameRate(24)
  app.setDisplayMode(800, 550, false)
  app.start()
}
