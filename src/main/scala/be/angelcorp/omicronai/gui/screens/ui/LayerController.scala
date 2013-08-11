package be.angelcorp.omicronai.gui.screens.ui

import scala.collection.JavaConverters._
import de.lessvoid.nifty.{Nifty, NiftyEvent, NiftyEventSubscriber}
import de.lessvoid.nifty.controls._
import com.lyndir.omicron.api.model.LevelType
import be.angelcorp.omicronai.gui.layerRender.{ObjectLayer, FieldOfView, GridRenderer, LayerRenderer}
import be.angelcorp.omicronai.gui.{ViewPort, GuiController, AiGui}
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import org.newdawn.slick.{Graphics, Color}
import scala.collection.mutable
import akka.actor.ActorRef
import be.angelcorp.omicronai.assets.Asset
import akka.util.Timeout
import be.angelcorp.omicronai.gui.screens.ui
import be.angelcorp.omicronai.SupervisorMessage
import be.angelcorp.omicronai.agents.ValidateAction

class LayerController(gui: AiGui, nifty: Nifty) extends GuiController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  lazy val uiScreen        = nifty.getScreen(UserInterface.name)
  lazy val layerUpButton   = uiScreen.findNiftyControl("layerUpButton",   classOf[Button])
  lazy val layerLabel      = uiScreen.findNiftyControl("layerLabel",      classOf[Label])
  lazy val layerDownButton = uiScreen.findNiftyControl("layerDownButton", classOf[Button])

  @NiftyEventSubscriber(id = "layerUpButton")
  def layerUpButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      gui.view.activeLayer = math.min( gui.view.activeLayer + 1, LevelType.values().size - 1 )
      layerLabel.setText( LevelType.values()(gui.view.activeLayer).getName )
    case _ =>
  }

  @NiftyEventSubscriber(id = "layerDownButton")
  def layerDownButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      gui.view.activeLayer = math.max( gui.view.activeLayer - 1, 0 )
      layerLabel.setText( LevelType.values()(gui.view.activeLayer).getName )
    case _ =>
  }

  @NiftyEventSubscriber(id = "layerList")
  def updateLayers(id: String, event: ListBoxSelectionChangedEvent[LayerRenderer]) {
    event.getListBox.getItems.asScala.zipWithIndex.foreach( entry => {
      val renderer = entry._1
      val index    = entry._2
      if ( event.getSelectionIndices.contains(index) ) {
        if (!gui.renderLayers.contains(renderer)) {
          logger.info("Enabling extra layer info: " + renderer.toString )
          gui.renderLayers.append( renderer )
          renderer.update( gui.view )
        }
      } else {
        if (gui.renderLayers.contains(renderer)) {
          logger.info("Disabling extra layer info: " + renderer.toString )
          gui.renderLayers.remove( gui.renderLayers.indexOf(renderer) )
        }
      }
    } )
  }

  override def populate() {
    val pike = gui.pike

    val lb = uiScreen.findNiftyControl("layerList", classOf[ListBox[LayerRenderer]])
    lb.addItem( new GridRenderer(pike, Color.white) )
    lb.addItem( new FieldOfView(pike, Color.white)     )
    lb.addItem( new ObjectLayer(pike, go => go.getPlayer == pike, "Friendly units", Color.green, Color.transparent ) )
    lb.addItem( new ObjectLayer(pike, go => go.getPlayer != pike, "Enemy units",    Color.red,   Color.transparent ) )
    lb.addItem( new LayerRenderer {
      val logger = Logger( LoggerFactory.getLogger( getClass ) )
      def selected = {
        val messagesList = uiScreen.findNiftyControl("messageList", classOf[ListBox[SupervisorMessage]])
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
    gui.renderLayers.appendAll(lb.getSelection.asScala)
  }
}