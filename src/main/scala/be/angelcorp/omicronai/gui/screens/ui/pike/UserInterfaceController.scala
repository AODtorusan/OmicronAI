package be.angelcorp.omicronai.gui.screens.ui.pike

import scala.Some
import scala.concurrent.duration._
import akka.pattern._
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.controls.{ButtonClickedEvent, Button}
import de.lessvoid.nifty.{NiftyEvent, NiftyEventSubscriber}
import be.angelcorp.omicronai.configuration.Configuration._
import be.angelcorp.omicronai.gui.GuiController
import be.angelcorp.omicronai.ai.pike.PikeInterface
import be.angelcorp.omicronai.ai.pike.agents.GetAsset
import be.angelcorp.omicronai.gui.screens.ui.UserInterface
import be.angelcorp.omicronai.bridge.Asset

class UserInterfaceController(val pikeInt: PikeInterface, unitController: UnitTreeController) extends GuiController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit val timeout: Timeout = config.gui.messageTimeout seconds;

  lazy val uiScreen     = pikeInt.nifty.getScreen(UserInterface.name)
  lazy val autoButton   = uiScreen.findNiftyControl("autoButton",   classOf[Button])
  lazy val centerButton = uiScreen.findNiftyControl("centerButton", classOf[Button])

  def selectedUnit = unitController.selectedUnit

  @NiftyEventSubscriber(id = "autoButton")
  def autoButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      selectedUnit match {
        case Some(unit) =>
          pikeInt.pike.supervisor.toggleAuto( unit )
          pikeInt.updateUI()
        case None =>
      }
    case _ =>
  }

  @NiftyEventSubscriber(id = "centerButton")
  def centerButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      selectedUnit match {
        case Some(unit) =>
          import scala.concurrent.ExecutionContext.Implicits.global
          for ( asset <- ask(unit, GetAsset()).mapTo[Asset] ) pikeInt.gui.view.centerOn( asset.location )
        case None =>
      }
    case _ =>
  }

  @NiftyEventSubscriber(id = "exitButton")
  def exit(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      pikeInt.nifty.exit()
      System.exit(0)
    case _ =>
  }

  override def updateUI() {
    selectedUnit match {
      case Some(unit) =>
        enable(autoButton)
        autoButton.setText( "Auto " + (if (pikeInt.pike.supervisor.isOnAuto( unit )) "(on)" else "(off)") )
        enable(centerButton)
      case _ =>
        disable(autoButton)
        disable(centerButton)
    }
  }
}
