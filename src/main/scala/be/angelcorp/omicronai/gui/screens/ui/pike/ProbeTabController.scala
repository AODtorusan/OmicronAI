package be.angelcorp.omicronai.gui.screens.ui.pike

import scala.Some
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import akka.util.Timeout
import akka.actor.ActorRef
import akka.pattern._
import de.lessvoid.nifty.NiftyEventSubscriber
import de.lessvoid.nifty.controls.{TreeBox, TreeItemSelectionChangedEvent, TreeItem}
import org.newdawn.slick.Graphics
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.gui._
import be.angelcorp.omicronai.gui.layerRender.LayerRenderer
import be.angelcorp.omicronai.configuration.Configuration
import Configuration._
import be.angelcorp.omicronai.metadata.MetaData
import be.angelcorp.omicronai.ai.pike.PikeInterface
import be.angelcorp.omicronai.ai.pike.agents.ListMetadata
import be.angelcorp.omicronai.gui.screens.ui.UserInterface

class ProbeTabController(val pikeInt: PikeInterface, unitController: UnitTreeController) extends GuiController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit val timeout: Timeout = config.gui.messageTimeout seconds;

  lazy val uiScreen   = pikeInt.nifty.getScreen(UserInterface.name)
  lazy val probesTree = uiScreen.findNiftyControl("probesTree", classOf[TreeBox[LayerRenderer]])

  def selectedUnit    = unitController.selectedUnit

  def treeContains[T]( tree: TreeItem[T], element: T ): Boolean =
    tree.getValue == element || tree.iterator().asScala.exists( t => treeContains(t, element) )

  @NiftyEventSubscriber(id = "probesTree")
  def updateProbesTree(id: String, event: TreeItemSelectionChangedEvent[LayerRenderer]) {
    event.getTreeBoxControl.getItems.asScala.foreach( p => {
      val i = pikeInt.activeLayers.indexOf(p.getValue)
      if (i != -1) pikeInt.activeLayers.remove( i )
    } )
    selectedProbe match {
      case Some(probe) =>
        pikeInt.activeLayers.append( probe )
        probe.update( pikeInt.gui.view )
      case None =>
    }
  }

  def selectedProbe   = probesTree.getSelection.asScala.headOption.map( _.getValue )

  class WrappedLayer(l: LayerRenderer, s: String) extends LayerRenderer {
    override def toString = s
    override def update(view: ViewPort) = l.update(view)
    def render(g: Graphics, view: ViewPort) = l.render(g, view)
  }

  def probesFor( unit: ActorRef ): TreeItem[LayerRenderer] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val root = new TreeItem[LayerRenderer](null)

    (unit ? ListMetadata()).mapTo[Iterable[MetaData]].onSuccess( {
      case metadatas =>
        for (metadata <- metadatas) {
          val metadataNode = new TreeItem[LayerRenderer]( new LayerRenderer {
            def render(g: Graphics, view: ViewPort) {}
            override def toString: String = metadata.title
          } )
          root.addTreeItem(metadataNode)
          for (layer <- metadata.layers)
            metadataNode.addTreeItem( new TreeItem[LayerRenderer]( new WrappedLayer(layer._2, layer._1) ) )
        }
    } )
    root
  }

  override def updateUI() {
    probesTree.getItems.asScala.foreach( p => {
      val i = pikeInt.activeLayers.indexOf(p.getValue)
      if (i != -1) pikeInt.activeLayers.remove( i )
    } )
    selectedUnit match {
      case Some(unit) =>
        val tree = probesFor(unit)
       probesTree.setTree( tree )
      case _ =>
       probesTree.setTree( new TreeItem[LayerRenderer]() )
    }
  }

}
