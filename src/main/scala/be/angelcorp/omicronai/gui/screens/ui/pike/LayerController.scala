package be.angelcorp.omicronai.gui.screens.ui.pike

import scala.collection.JavaConverters._
import de.lessvoid.nifty.{NiftyEvent, NiftyEventSubscriber}
import de.lessvoid.nifty.controls._
import de.lessvoid.nifty.controls.ListBox.SelectionMode
import org.slf4j.LoggerFactory
import org.newdawn.slick.{Graphics, Color}
import com.lyndir.omicron.api.model.LevelType
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicronai.ai.pike.PikeInterface
import be.angelcorp.omicronai.gui.GuiController
import be.angelcorp.omicronai.gui.layerRender._
import be.angelcorp.omicronai.gui.screens.ui.UserInterface
import be.angelcorp.omicronai.world.SubWorld

class LayerController(val pikeInt: PikeInterface) extends GuiController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  lazy val uiScreen        = pikeInt.nifty.getScreen(UserInterface.name)
  lazy val layerUpButton   = uiScreen.findNiftyControl("layerUpButton",   classOf[Button])
  lazy val layerLabel      = uiScreen.findNiftyControl("layerLabel",      classOf[Label])
  lazy val layerDownButton = uiScreen.findNiftyControl("layerDownButton", classOf[Button])

  @NiftyEventSubscriber(id = "layerUpButton")
  def layerUpButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      pikeInt.gui.view.activeLayer = math.min( pikeInt.gui.view.activeLayer + 1, LevelType.values().size - 1 )
      layerLabel.setText( LevelType.values()(pikeInt.gui.view.activeLayer).getName )
    case _ =>
  }

  @NiftyEventSubscriber(id = "layerDownButton")
  def layerDownButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      pikeInt.gui.view.activeLayer = math.max( pikeInt.gui.view.activeLayer - 1, 0 )
      layerLabel.setText( LevelType.values()(pikeInt.gui.view.activeLayer).getName )
    case _ =>
  }

  @NiftyEventSubscriber(id = "layerList")
  def updateLayers(id: String, event: ListBoxSelectionChangedEvent[LayerRenderer]) {
    event.getListBox.getItems.asScala.zipWithIndex.foreach( entry => {
      val renderer = entry._1
      val index    = entry._2
      if ( event.getSelectionIndices.contains(index) ) {
        if (!pikeInt.activeLayers.contains(renderer)) {
          logger.info("Enabling extra layer info: " + renderer.toString )
          pikeInt.activeLayers.append( renderer )
          renderer.viewChanged( pikeInt.gui.view )
        }
      } else {
        if (pikeInt.activeLayers.contains(renderer)) {
          logger.info("Disabling extra layer info: " + renderer.toString )
          pikeInt.activeLayers.remove( pikeInt.activeLayers.indexOf(renderer) )
        }
      }
    } )
  }

  override def populate() {
    val pike = pikeInt.pike

    val lb = uiScreen.findNiftyControl("layerList", classOf[ListBox[LayerRenderer]])
    lb.addItem( new TexturedWorldRenderer(pike.world) )
    lb.addItem( new ObjectLayer(pike.world, go => go.getOwner.isPresent  && go.getOwner.get() == pike, "Friendly units", Color.green, Color.transparent ) )
    lb.addItem( new ObjectLayer(pike.world, go => !go.getOwner.isPresent || go.getOwner.get() != pike, "Enemy units",    Color.red,   Color.transparent ) )
    lb.addItem( new FieldOfView(pike.world)           )
    lb.addItem( new GridRenderer(pike)                )
    lb.addItem( new LayerRenderer {
      val logger = Logger( LoggerFactory.getLogger( getClass ) )
      def selected = {
        ???
//        val messagesList = uiScreen.findNiftyControl("messageList", classOf[ListBox[SupervisorMessage]])
//        messagesList.getSelection.asScala.headOption.flatMap( _.message match {
//          case m: ValidateAction => Some(m)
//          case _ => None
//        } )
      }
      override def prepareRender(subWorld: SubWorld, layer: Int) {}
      override def render(g: Graphics) {
//        selected match {
//          case Some(a) =>
//            logger.warn("No way to retrieve metadata")
//          for (m <- a.action.metadata; l <- m.layers) l._2.render(g, view)
//          case None =>
//        }
      }
      override def toString: String = "Planned action preview"
    } )
    lb.changeSelectionMode( SelectionMode.Multiple, false )
    for (layer <- lb.getItems.asScala) {
      lb.selectItem( layer )
      pikeInt.activeLayers.append(layer)
    }
  }
}
