package be.angelcorp.omicronai.gui.screens.ui

import scala.collection.JavaConverters._
import akka.actor.ActorRef
import de.lessvoid.nifty.{NiftyEvent, NiftyEventSubscriber, Nifty}
import de.lessvoid.nifty.controls.{TreeItem, ButtonClickedEvent, Button, ListBox}
import be.angelcorp.omicronai.gui.{GuiSupervisorInterface, AiGui, GuiController}
import be.angelcorp.omicronai.SupervisorMessage
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

class MessageTabController(gui: AiGui, nifty: Nifty, unitController: UnitTreeController) extends GuiController with GuiSupervisorInterface {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  lazy val uiScreen     = nifty.getScreen(UserInterface.name)
  lazy val messageList  = uiScreen.findNiftyControl("messageList",  classOf[ListBox[SupervisorMessage]])
  lazy val acceptButton = uiScreen.findNiftyControl("acceptButton", classOf[Button])
  lazy val rejectButton = uiScreen.findNiftyControl("rejectButton", classOf[Button])
  lazy val modifyButton = uiScreen.findNiftyControl("modifyButton", classOf[Button])
  lazy val addButton    = uiScreen.findNiftyControl("addButton",    classOf[Button])

  def selectedMessage   = messageList.getSelection.asScala.headOption
  def selectedUnit      = unitController.selectedUnit

  @NiftyEventSubscriber(id = "acceptButton")
  def acceptButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => selectedMessage match {
      case Some(message) =>
        messageList.removeItem(message)
        gui.supervisor.acceptMessage( message )
      case None =>
    }
    case _ =>
  }

  @NiftyEventSubscriber(id = "rejectButton")
  def rejectButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => selectedMessage match {
      case Some(message) =>
        messageList.removeItem(message)
        gui.supervisor.rejectMessage(message)
      case None =>
    }
    case _ =>
  }

  @NiftyEventSubscriber(id = "modifyButton")
  def modifyButtonAction(id: String, event: NiftyEvent) {
  }

  @NiftyEventSubscriber(id = "addButton")
  def addButtonAction(id: String, event: NiftyEvent) {
  }


  override def updateUI() {
    messageList.clear()
    selectedUnit match {
      case Some(unit) =>
        enable(acceptButton)
        enable(rejectButton)
        enable(modifyButton)
        messageList.addAllItems( gui.supervisor.messagesFor(unit).asJava )
      case _ =>
        disable(acceptButton)
        disable(rejectButton)
        disable(modifyButton)
        disable(addButton)
    }
  }

  def messageBuffered( msg: SupervisorMessage ) = selectedUnit match {
    case Some(unit) if unit == msg.destination =>
      messageList.addItem( msg )
    case _ =>
  }

  def messageSend(msg: SupervisorMessage) = selectedUnit match {
    case Some(unit) if msg.destination == unit =>
      messageList.removeItem( msg )
  }

}
