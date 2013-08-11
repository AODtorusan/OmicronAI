package be.angelcorp.omicronai.gui.screens.ui

import scala.Some
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import akka.util.Timeout
import akka.actor.ActorRef
import akka.pattern._
import de.lessvoid.nifty.{NiftyEventSubscriber, Nifty}
import de.lessvoid.nifty.controls.{TreeBox, TreeItemSelectionChangedEvent, TreeItem}
import org.newdawn.slick.Graphics
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.gui._
import be.angelcorp.omicronai.gui.layerRender.LayerRenderer
import be.angelcorp.omicronai.Settings._
import be.angelcorp.omicronai.agents.ListMetadata
import be.angelcorp.omicronai.metadata.MetaData

class ProbeTabController(gui: AiGui, nifty: Nifty, unitController: UnitTreeController) extends GuiController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit val timeout: Timeout = settings.ai.messageTimeout seconds;

  lazy val uiScreen   = nifty.getScreen(UserInterface.name)
  lazy val probesTree = uiScreen.findNiftyControl("probesTree", classOf[TreeBox[LayerRenderer]])

  def selectedUnit    = unitController.selectedUnit

  def treeContains[T]( tree: TreeItem[T], element: T ): Boolean =
    tree.getValue == element || tree.iterator().asScala.exists( t => treeContains(t, element) )

  @NiftyEventSubscriber(id = "probesTree")
  def updateProbesTree(id: String, event: TreeItemSelectionChangedEvent[LayerRenderer]) {
    event.getTreeBoxControl.getItems.asScala.foreach( p => {
      val i = gui.renderLayers.indexOf(p.getValue)
      if (i != -1) gui.renderLayers.remove( i )
    } )
    selectedProbe match {
      case Some(probe) =>
        gui.renderLayers.append( probe )
        probe.update( gui.view )
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

    val metadatas = Await.result( (unit ? ListMetadata()).mapTo[Iterable[MetaData]], timeout.duration)
      for (metadata <- metadatas) {
        val metadataNode = new TreeItem[LayerRenderer]( new LayerRenderer {
          def render(g: Graphics, view: ViewPort) {}
          override def toString: String = metadata.title
        } )
        root.addTreeItem(metadataNode)
        for (layer <- metadata.layers)
          metadataNode.addTreeItem( new TreeItem[LayerRenderer]( new WrappedLayer(layer._2, layer._1) ) )
      }

    root
  }

  override def updateUI() {
    probesTree.getItems.asScala.foreach( p => {
      val i = gui.renderLayers.indexOf(p.getValue)
      if (i != -1) gui.renderLayers.remove( i )
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
