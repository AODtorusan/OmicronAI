package be.angelcorp.omicron.pike.gui

import scala.Some
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import akka.pattern._
import akka.actor.ActorRef
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.{NiftyEvent, NiftyEventSubscriber}
import de.lessvoid.nifty.controls._
import de.lessvoid.nifty.controls.treebox.TreeBoxControl
import be.angelcorp.omicron.base.configuration.Configuration.config
import be.angelcorp.omicron.pike.PikeInterface
import be.angelcorp.omicron.pike.agents.ListMembers
import be.angelcorp.omicron.base.gui.GuiController

class UnitTreeController(val pikeInt: PikeInterface) extends GuiController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit val timeout: Timeout = config.gui.messageTimeout seconds;
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val uiScreen       = pikeInt.nifty.getScreen(PikeUserInterface.screenId)
  lazy val unitTree       = uiScreen.findNiftyControl("unitTree",       classOf[TreeBox[ActorRef]])
  lazy val unitTreeUpdate = uiScreen.findNiftyControl("unitTreeUpdate", classOf[Button])

  def selectedUnit = unitTree.getSelection.asScala.headOption.map( _.getValue )

  @NiftyEventSubscriber(id = "unitTree")
  def updateUnitSelection( id: String, event: TreeItemSelectionChangedEvent[ActorRef] ) {
    selectedUnit match {
      case Some(unit) => pikeInt.updateUI()
      case None =>
    }
  }

  def buildTree(parent: TreeItem[ActorRef], actor: ActorRef) {
    val node = new TreeItem[ActorRef](actor)
    parent.addTreeItem(node)

    // Dirty trick to update the gui element
    unitTree.asInstanceOf[TreeBoxControl[ActorRef]].updateList( unitTree.getSelection.asScala.headOption.getOrElse(null))

    for (children <- (actor ? ListMembers()).mapTo[Iterable[ActorRef]])
      for (child <- children)
        buildTree(node, child)
  }

  def resetTree() {
    val root = new TreeItem[ActorRef](ActorRef.noSender)
    unitTree.setTree( root )
    buildTree(root, pikeInt.pike.admiralRef)
  }

  @NiftyEventSubscriber(id = "unitTreeUpdate")
  def unitTreeUpdateAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => resetTree()
    case _ =>
  }

  override def populate() {
    resetTree()
  }

}
