package be.angelcorp.omicronai.gui.screens.ui

import scala.Some
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import akka.pattern._
import akka.actor.ActorRef
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.{NiftyEvent, NiftyEventSubscriber, Nifty}
import de.lessvoid.nifty.controls._
import be.angelcorp.omicronai.gui.{AiGui, GuiController}
import be.angelcorp.omicronai.agents.ListMembers
import be.angelcorp.omicronai.Settings.settings

class UnitTreeController(gui: AiGui, nifty: Nifty) extends GuiController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit val timeout: Timeout = settings.ai.messageTimeout seconds;

  lazy val uiScreen       = nifty.getScreen(UserInterface.name)
  lazy val unitTree       = uiScreen.findNiftyControl("unitTree",       classOf[TreeBox[ActorRef]])
  lazy val unitTreeUpdate = uiScreen.findNiftyControl("unitTreeUpdate", classOf[Button])

  def selectedUnit = unitTree.getSelection.asScala.headOption.map( _.getValue )

  @NiftyEventSubscriber(id = "unitTree")
  def updateUnitSelection( id: String, event: TreeItemSelectionChangedEvent[ActorRef] ) {
    selectedUnit match {
      case Some(unit) => gui.updateUI()
      case None =>
    }
  }

  def buildTree(actor: ActorRef): Future[TreeItem[ActorRef]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val node = Future(new TreeItem[ActorRef](actor))
    for (n <- node; children <- (actor ? ListMembers()).mapTo[Iterable[ActorRef]])
      for (child <- children)
        for ( childNode <- buildTree(child) )
          n.addTreeItem( childNode )
    node
  }

  def resetTree() {
    val root = new TreeItem[ActorRef](ActorRef.noSender)
    root.addTreeItem( Await.result(buildTree(gui.pike.admiralRef), timeout.duration) )
    unitTree.setTree( root )
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
