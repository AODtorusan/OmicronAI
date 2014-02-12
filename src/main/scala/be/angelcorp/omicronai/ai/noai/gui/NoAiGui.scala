package be.angelcorp.omicronai.ai.noai.gui

import scala.Some
import scala.collection.mutable
import de.lessvoid.nifty.Nifty
import org.newdawn.slick.{Graphics, Color}
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.ai.noai.{NoAiGameListener, NoAi}
import be.angelcorp.omicronai.ai.noai.gui.screens.NoAiConstructionScreenController
import be.angelcorp.omicronai.bridge.Asset
import be.angelcorp.omicronai.gui._
import be.angelcorp.omicronai.gui.layerRender._
import be.angelcorp.omicronai.gui.slick.DrawStyle
import be.angelcorp.omicronai.world.SubWorld

class NoAiGui(val noai: NoAi, val frame: AiGuiOverlay, val nifty: Nifty) extends GuiInterface {
  val listener = new NoAiGameListener( this )
  frame.game.getController.addGameListener( listener )
  frame.input.inputHandlers.prepend( new NoAiInput(noai, this) )

  private val uiScreen = screens.NoAiUserInterface.screen(this)
  private val constructionScreen = screens.NoAiConstructionScreen.screen(this)
  nifty.addScreen( uiScreen.getScreenId, uiScreen )
  nifty.addScreen( constructionScreen.getScreenId, constructionScreen )
  nifty.gotoScreen( uiScreen.getScreenId )

  private val messages = mutable.ListBuffer[String]()
  private val messageLabel = uiScreen.findNiftyControl("messages", classOf[de.lessvoid.nifty.controls.Label])

  private val gridRenderer      = new GridRenderer(noai)
  private val resourceRenderer  = new ResourceRenderer(noai.world)

  protected[gui] var hideGame     = false
  protected[gui] var gridOn       = true
  protected[gui] var resourcesOn  = false

  private val staticLayers = mutable.ListBuffer[ LayerRenderer ]()
  staticLayers += new TexturedWorldRenderer( noai.world )
  staticLayers += new ObjectLayer( noai.world, o => o.getOwner.isPresent && o.getOwner.get() == noai, "Friendly units", Color.green, Color.green )
  staticLayers += new ObjectLayer( noai.world, o => o.getOwner.isPresent && o.getOwner.get() != noai, "Enemy units",    Color.red,   new Color(0.5f, 0f, 0f) )
  staticLayers += new LayerRenderer {
    // Renders the currently selected unit
    override def prepareRender(subWorld: SubWorld, layer: Int) {}
    override def render(g: Graphics) {
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
    val layers = mutable.ListBuffer[LayerRenderer]( staticLayers: _* )
    if (gridOn)                       layers += gridRenderer
    if (noai.plannedAction.isDefined) layers += noai.plannedAction.get.preview
    if (resourcesOn)                  layers += resourceRenderer
    layers
  }

}
