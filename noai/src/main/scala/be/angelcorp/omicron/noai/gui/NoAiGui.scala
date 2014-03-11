package be.angelcorp.omicron.noai.gui

import scala.collection.mutable
import akka.actor.{Actor, Props}
import org.newdawn.slick.{Graphics, Color}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicron.base.HexTile
import be.angelcorp.omicron.base.Conversions._
import be.angelcorp.omicron.base.bridge.Asset
import be.angelcorp.omicron.base.gui._
import be.angelcorp.omicron.base.gui.layerRender._
import be.angelcorp.omicron.base.gui.layerRender.renderEngine.{UnitProvider, TerrainProvider, RenderEngine}
import be.angelcorp.omicron.base.gui.slick.DrawStyle
import be.angelcorp.omicron.base.world.{GhostState, KnownState, SubWorld}
import be.angelcorp.omicron.noai.{GuiMessage, NoAiGameListener}

class NoAiGui(val controller: GuiController) extends NiftyGuiInterface {
  private val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def noai  = controller.noai
  def frame = controller.frame
  def nifty = controller.nifty

  val listener = new NoAiGameListener( this )
  frame.game.getController.addGameListener( listener )
  noai.actorSystem.actorOf( Props(classOf[NoAiInput], noai, this), name = "NoAI_input" )

  private val uiScreen = screens.NoAiUserInterface.screen(this)
  private val constructionScreen = screens.NoAiConstructionScreen.screen(this)
  private val messagesScreen = screens.NoAiMessagesScreen.screen(this)
  nifty.addScreen( uiScreen.getScreenId, uiScreen )
  nifty.addScreen( constructionScreen.getScreenId, constructionScreen )
  nifty.addScreen( messagesScreen.getScreenId, messagesScreen )
  nifty.gotoScreen( uiScreen.getScreenId )

  private val messages = mutable.ListBuffer[String]()
  private val messageLabel = uiScreen.findNiftyControl("messages", classOf[de.lessvoid.nifty.controls.Label])

  noai.actorSystem.actorOf( Props( new Actor {
    override def preStart() {
      controller.guiMessages.subscribe(context.self, classOf[GuiMessage])
    }
    override def receive = {
      case m: GuiMessage =>
        messages.prepend( m.message )
        if (messages.size > 10) messages.remove(messages.size - 1)
        messageLabel.setText( messages.mkString("\n") )
    }
  } ) )

  protected[noai] val gridRenderer      = new TogglableLayerRenderer(new GridRenderer(noai))
  protected[noai] val resourceRenderer  = new TogglableLayerRenderer(new ResourceRenderer(noai.world), false)

  frame.renderer.spriteProvider += new TerrainProvider
  frame.renderer.spriteProvider += new UnitProvider

  val overlays = frame.renderer.overlays.getOrElseUpdate( RenderEngine.AboveSpace, mutable.ListBuffer[LayerRenderer]() )
  overlays += gridRenderer
  overlays += new LayerRenderer {
    // Renders a border around the units
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
  overlays += resourceRenderer
  overlays += new LayerRenderer {
    // Renders the currently selected unit proposed action
    override def viewChanged(view: ViewPort): Unit =
      controller.plannedAction.foreach( _.preview.viewChanged(view) )
    override def prepareRender(subWorld: SubWorld, layer: Int) =
      controller.plannedAction.foreach( _.preview.prepareRender(subWorld, layer) )
    override def render(g: Graphics) =
      controller.plannedAction.foreach( _.preview.render(g) )
  }
  overlays += new LayerRenderer {
    // Renders the currently selected unit
    override def prepareRender(subWorld: SubWorld, layer: Int) {}
    override def render(g: Graphics) {
      controller.selected.foreach( unit =>
        Canvas.render(g, unit.location.get, new DrawStyle(Color.orange, 3.0f), Color.transparent)
      )
    }
  }
  overlays += new FieldOfView( noai.world )

  new Thread {
    override def run() {
      // TODO: Fix this quickfix : )
      Thread.sleep(1000)
      frame.view.centerOn( noai.units.head.location.get )
    }
  }.start()

  def moveTo(asset: Asset) {
    val loc = asset.location.get
    moveTo( loc.h )
    frame.view.centerOn( loc )
  }

  def moveTo(h: Int) {
    val fromLayer = frame.view.activeLayer
    frame.view.activeLayer = h
    controller.guiMessages.publish( LevelChanged(fromLayer, h) )
  }

  def moveUp()   = moveTo( math.min( frame.view.activeLayer + 1, noai.gameSize.hSize - 1) )
  def moveDown() = moveTo( math.max( frame.view.activeLayer - 1, 0 ) )

}
