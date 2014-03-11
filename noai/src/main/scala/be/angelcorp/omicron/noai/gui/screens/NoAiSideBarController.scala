package be.angelcorp.omicron.noai.gui.screens

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.controls._
import de.lessvoid.nifty.{NiftyEvent, NiftyEventSubscriber}
import be.angelcorp.omicron.base.gui.GuiController
import be.angelcorp.omicron.noai.PlainMessage
import com.lyndir.omicron.api.model._
import be.angelcorp.omicron.base.Auth
import be.angelcorp.omicron.noai.PlainMessage
import scala.Some


class NoAiSideBarController(ui: NoAiUserInterfaceController) extends GuiController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  lazy val uiScreen         = ui.nifty.getScreen(NoAiUserInterface.screenId)
  lazy val layerLabel       = uiScreen.findNiftyControl("layerLabel",      classOf[Label])
  lazy val unitDescription  = uiScreen.findNiftyControl("unitDescription", classOf[Label])

  @NiftyEventSubscriber(id = "menuButton")
  def menuButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => ui.gui.controller.guiMessages.publish( new PlainMessage("Main menu not yet implemented!") )
    case _ =>
  }

  @NiftyEventSubscriber(id = "messagesButton")
  def messagesAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => ui.gui.gotoScreen( NoAiMessagesScreen )
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

  def updateSelectedUnitStats() {
    val descr = new StringBuilder
    ui.gui.controller.selected match {
      case Some(unit) =>
        val name = unit.name
        descr ++= name
        descr ++= (if (name == unit.gameObject.getType.getTypeName) "\n" else s" (${unit.gameObject.getType.getTypeName})\n")
        val modules = unit.modules.toList.sortBy( _.getType.getModuleType.getName )
        val auth = ui.gui.controller.noai.getAuth
        modules.foreach( module => descr ++= moduleInfo(auth, module) + "\n" )
      case _ =>
        descr ++= "No unit selected"
    }
    unitDescription.setText(descr.result())
  }

  def moduleInfo( auth: Auth, module: IModule ): String = auth{ module match {
    case b: BaseModule =>
      s"""Health: ${b.getRemainingHealth}/${b.getMaxHealth}
         |Armor: ${b.getArmor}
         |Scan: ${b.getViewRange}""".stripMargin
    case c: ConstructorModule =>
      s"""Builder for ${c.getBuildsModule.getModuleType.getSimpleName}
         |  Available ${c.getRemainingSpeed}/${c.getBuildSpeed}""".stripMargin
    case c: ContainerModule =>
      s"""${c.getResourceType} stock: ${c.getStock}/${c.getCapacity}"""
    case e: ExtractorModule =>
      s"""${e.getResourceType} extractor (${e.getSpeed})"""
    case m: MobilityModule =>
      s"""Speed: ${m.getRemainingSpeed}/???"""
    case w: WeaponModule =>
      s"""Weapon ${w.getClass.getSimpleName}
         |  Ammo: ${w.getAmmunition}/${w.getAmmunitionLoad}
         |  Power: ${w.getFirePower} (σ ${w.getVariance})
         |  Range: ${w.getRange}
         |  Shots: ${w.getRepeat}/${w.getRepeated}""".stripMargin
    case _ => module.toString
  } }

}
