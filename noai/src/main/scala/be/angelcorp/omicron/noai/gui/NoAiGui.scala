package be.angelcorp.omicron.noai.gui

import scala.Some
import scala.collection.mutable
import akka.actor.Props
import de.lessvoid.nifty.Nifty
import org.newdawn.slick.{Graphics, Color}
import com.lyndir.omicron.api.model.LevelType
import be.angelcorp.omicron.base.{Present, HexTile, Location}
import be.angelcorp.omicron.base.bridge.Asset
import be.angelcorp.omicron.base.gui.{Canvas, GuiInterface, AiGuiOverlay}
import be.angelcorp.omicron.base.gui.layerRender._
import be.angelcorp.omicron.base.gui.slick.DrawStyle
import be.angelcorp.omicron.base.world.{GhostState, KnownState, SubWorld}
import be.angelcorp.omicron.base.Conversions._
import be.angelcorp.omicron.noai.{NoAiGameListener, NoAi}
import be.angelcorp.omicron.noai.gui.screens.{NoAiUserInterfaceController, NoAiConstructionScreenController}
import be.angelcorp.omicron.base.gui.layerRender.renderEngine.RenderEngine

class NoAiGui(val noai: NoAi, val frame: AiGuiOverlay, val nifty: Nifty) extends GuiInterface {
  val listener = new NoAiGameListener( this )
  frame.game.getController.addGameListener( listener )
  noai.actorSystem.actorOf( Props(classOf[NoAiInput], noai, this), name = "NoAI_input" )

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
  staticLayers += new RenderEngine()
  staticLayers += new LayerRenderer {
    val unknown = new DrawStyle(Color.white, 3.0f)
    var tiles: Map[DrawStyle, Iterable[HexTile]] = Map.empty
    override def prepareRender(subWorld: SubWorld, layer: Int) = {
      tiles = subWorld.states.flatten.flatMap {
        case (loc, KnownState(_,Some(obj),_)) =>
          Some(HexTile(loc) -> ( obj.owner match {
            case Some( owner ) => DrawStyle(owner.getPrimaryColor, 3.0f)
            case _ => unknown
          } ) )
        case (loc, GhostState(_,Some(obj),_)) =>
          Some(HexTile(loc) -> ( obj.owner match {
            case Some( owner ) => DrawStyle(owner.getPrimaryColor, 3.0f)
            case _ => unknown
          } ) )
        case _ => None
      }.toList.groupBy( _._2 ).mapValues( _.map( _._1 ) )
    }
    override def render(g: Graphics) = {
      for ((color, locations) <- tiles)
        Canvas.render(g, locations, color)

    }
  }
  staticLayers += new LayerRenderer {
    // Renders the currently selected unit
    override def prepareRender(subWorld: SubWorld, layer: Int) {}
    override def render(g: Graphics) {
      noai.selected match {
        case Some( unit ) =>
          Canvas.render(g, unit.location.get, new DrawStyle(Color.orange, 3.0f), Color.transparent)
        case _ =>
      }
    }
  }
  staticLayers += new FieldOfView( noai.world )

  new Thread {
    override def run() {
      // TODO: Fix this quickfix : )
      Thread.sleep(1000)
      frame.view.centerOn( noai.units.head.location.get )
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
    if (gridOn) layers += gridRenderer
    noai.plannedAction.foreach( plan => layers += plan.preview )
    if (resourcesOn) layers += resourceRenderer
    layers
  }

  def moveTo(asset: Asset) {
    val loc = asset.location.get
    moveTo( loc.h )
    frame.view.centerOn( loc )
  }

  def moveTo(h: Int) {
    frame.view.activeLayer = h
    uiScreen.getScreenController.asInstanceOf[NoAiUserInterfaceController].sidebarController.layerLabel.setText(
      LevelType.values()(h).getName
    )
  }

  def moveUp()   = moveTo( math.min( frame.view.activeLayer + 1, noai.gameSize.hSize - 1) )
  def moveDown() = moveTo( math.max( frame.view.activeLayer - 1, 0 ) )

}
