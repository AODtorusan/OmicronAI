package be.angelcorp.omicron.noai.gui.screens

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.controls._
import de.lessvoid.nifty.{NiftyEvent, NiftyEventSubscriber}
import be.angelcorp.omicron.base.gui.GuiController


class NoAiSideBarController(ui: NoAiUserInterfaceController) extends GuiController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  lazy val uiScreen         = ui.nifty.getScreen(NoAiUserInterface.name)
  lazy val menuButton       = uiScreen.findNiftyControl("menuButton",       classOf[Button])
  lazy val endTurnButton    = uiScreen.findNiftyControl("endTurnButton",    classOf[Button])
  lazy val layerUpButton    = uiScreen.findNiftyControl("layerUpButton",    classOf[Button])
  lazy val layerLabel       = uiScreen.findNiftyControl("layerLabel",       classOf[Label ])
  lazy val layerDownButton  = uiScreen.findNiftyControl("layerDownButton",  classOf[Button])

  @NiftyEventSubscriber(id = "menuButton")
  def menuButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => ui.gui.message("Main menu not yet implemented!")
    case _ =>
  }

  @NiftyEventSubscriber(id = "layerUpButton")
  def layerUpButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => ui.gui.moveUp()
    case _ =>
  }

  @NiftyEventSubscriber(id = "layerDownButton")
  def layerDownButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => ui.gui.moveDown()
    case _ =>
  }

  @NiftyEventSubscriber(id = "endTurnButton")
  def centerButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => ui.gui.noai.endTurn()
    case _ =>
  }

  @NiftyEventSubscriber(id = "gridButton")
  def gridButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => ui.gui.gridOn = !ui.gui.gridOn
    case _ =>
  }

  @NiftyEventSubscriber(id = "resourceButton")
  def resourceButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => ui.gui.resourcesOn = !ui.gui.resourcesOn
    case _ =>
  }
}