package be.angelcorp.omicronai.ai.noai.gui

import de.lessvoid.nifty.{NiftyEventAnnotationProcessor, Nifty}
import be.angelcorp.omicronai.gui._
import be.angelcorp.omicronai.gui.layerRender._
import be.angelcorp.omicronai.ai.noai.{NoAiGameListener, NoAi}
import scala.collection.mutable.ListBuffer
import org.newdawn.slick.{Graphics, Color}
import be.angelcorp.omicronai.ai.noai.gui.screens.{NoAiConstructionScreenController, NoAiPopupController, NoAiSideBarController}
import com.lyndir.omicron.api.model.LevelType
import scala.collection.mutable
import scala.Some
import javax.swing.SwingUtilities
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.{HexTile, Location}
import be.angelcorp.omicronai.gui.textures.Textures
import be.angelcorp.omicronai.gui.slick.DrawStyle
import be.angelcorp.omicronai.world.{GhostState, KnownState}
import scala.concurrent._
import java.util.concurrent.{TimeoutException, TimeUnit}
import scala.concurrent.duration.Duration
import be.angelcorp.omicronai.world.GhostState
import scala.Some
import be.angelcorp.omicronai.world.KnownState
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

class NoAiGui(val noai: NoAi, val frame: AiGuiOverlay, val nifty: Nifty) extends GuiInterface {
  val listener = new NoAiGameListener( this )
  frame.game.getController.addGameListener( listener )
  frame.input.inputHandlers.prepend( new NoAiInput(noai, this) )

  private val uiScreen = screens.NoAiUserInterface.screen(this)
  private val constructionScreen = screens.NoAiConstructionScreen.screen(this)
  nifty.addScreen( uiScreen.getScreenId, uiScreen )
  nifty.addScreen( constructionScreen.getScreenId, constructionScreen )
  nifty.gotoScreen( uiScreen.getScreenId )

  private val messages = ListBuffer[String]()
  private val messageLabel = uiScreen.findNiftyControl("messages", classOf[de.lessvoid.nifty.controls.Label])
  private val gridRenderer = new GridRenderer(noai)

  protected[gui] var hideGame = false
  protected[gui] var gridOn   = true

  private val staticLayers = mutable.ListBuffer[ LayerRenderer ]()
  staticLayers += new LayerRenderer {
    val logger = Logger( LoggerFactory.getLogger( getClass ) )
    override def render(g: Graphics, view: ViewPort) = {
      Textures.get("terrain.grass") foreach {
        img =>
          val futureStates = noai.world.statesOf(view.tilesInView.toList)
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
  }
  staticLayers += new ObjectLayer( noai.world, o => o.getOwner.isPresent && o.getOwner.get() == noai, "Friendly units", Color.green, Color.green )
  staticLayers += new ObjectLayer( noai.world, o => o.getOwner.isPresent && o.getOwner.get() != noai, "Enemy units",    Color.red,   new Color(0.5f, 0f, 0f) )
  staticLayers += new LayerRenderer {
    // Renders the currently selected unit
    def render(g: Graphics, view: ViewPort) {
      noai.selected match {
        case Some( unit ) => new Canvas(unit.location) {
          override def borderStyle: DrawStyle = new DrawStyle(Color.orange, 3.0f)
        }.render(g)
        case _ =>
      }
    }
  }
  staticLayers += new FieldOfView( noai.world )

  new Thread {
    override def run() {
      // TODO: Fix this quickfix : )
      Thread.sleep(1000)
      frame.view.centerOn( noai.units.head.location )
    }
  }.start()

  def gotoConstructionScreen(builder: Asset, target: Location) {
    nifty.gotoScreen( constructionScreen.getScreenId )
    constructionScreen.getScreenController.asInstanceOf[NoAiConstructionScreenController].populate(builder, target)
    new Thread { override def run(): Unit = {
      Thread.sleep(1000)
      hideGame = true
    } }.start()
  }

  def gotoUserInterface() {
    hideGame = false
    nifty.gotoScreen( uiScreen.getScreenId )
  }
  
  def message( msg: String ) {
    messages.prepend( msg )
    if (messages.size > 10) messages.remove(messages.size - 1)
    messageLabel.setText( messages.mkString("\n") )
  }

  def activeLayers: Seq[LayerRenderer] = if (hideGame) Nil else {
    val layers = ListBuffer[LayerRenderer]( staticLayers: _* )
    if (gridOn)                       layers += gridRenderer
    if (noai.plannedAction.isDefined) layers += noai.plannedAction.get.preview
    layers
  }

}
