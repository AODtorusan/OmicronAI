package be.angelcorp.omicronai.gui.screens.ui

import scala.Some
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern._
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.controls.{ButtonClickedEvent, Button}
import de.lessvoid.nifty.{NiftyEvent, NiftyEventSubscriber, Nifty}
import be.angelcorp.omicronai.Settings._
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.agents.GetAsset
import be.angelcorp.omicronai.gui.{AiGui, GuiController, GuiSupervisorInterface}

class UserInterfaceController(gui: AiGui, nifty: Nifty, unitController: UnitTreeController) extends GuiController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit val timeout: Timeout = settings.ai.messageTimeout seconds;

  lazy val uiScreen     = nifty.getScreen(UserInterface.name)
  lazy val autoButton   = uiScreen.findNiftyControl("autoButton",   classOf[Button])
  lazy val centerButton = uiScreen.findNiftyControl("centerButton", classOf[Button])

  def selectedUnit = unitController.selectedUnit

  @NiftyEventSubscriber(id = "autoButton")
  def autoButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      selectedUnit match {
        case Some(unit) =>
          gui.supervisor.toggleAuto( unit )
          gui.updateUI()
        case None =>
      }
    case _ =>
  }

  @NiftyEventSubscriber(id = "centerButton")
  def centerButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      selectedUnit match {
        case Some(unit) =>
          try {
            val asset = Await.result( ask(unit, GetAsset()), timeout.duration).asInstanceOf[Asset]
            gui.view.centerOn( asset.location )
          } catch {
            case e: Throwable => logger.info(s"Cannot center on unit ($unit), it does not have an asset or was not received in time!")
          }
        case None =>
      }
    case _ =>
  }

  @NiftyEventSubscriber(id = "exitButton")
  def exit(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      nifty.exit()
      System.exit(0)
    case _ =>
  }

  override def updateUI() {
    selectedUnit match {
      case Some(unit) =>
        enable(autoButton)
        autoButton.setText( "Auto " + (if (gui.supervisor.isOnAuto( unit )) "(on)" else "(off)") )
        enable(centerButton)
      case _ =>
        disable(autoButton)
        disable(centerButton)
    }
  }
}
