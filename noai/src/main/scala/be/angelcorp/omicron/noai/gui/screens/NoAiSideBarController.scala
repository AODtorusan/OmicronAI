package be.angelcorp.omicron.noai.gui.screens

import scala.Some
import akka.actor.Actor
import de.lessvoid.nifty.controls._
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import be.angelcorp.omicron.base.Auth
import be.angelcorp.omicron.base.bridge.GameListenerMessage
import be.angelcorp.omicron.base.util.GenericEventBus
import be.angelcorp.omicron.noai.PlainMessage
import be.angelcorp.omicron.noai.gui.{LevelChanged, SelectionChanged}


class NoAiSideBarController(ui: NoAiUserInterfaceController, protected val guiBus: GenericEventBus) extends Actor {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  lazy val uiScreen         = ui.nifty.getScreen(NoAiUserInterface.screenId)
  lazy val menu             = uiScreen.findNiftyControl("menuButton",      classOf[Button])
  lazy val messages         = uiScreen.findNiftyControl("messagesButton",  classOf[Button])
  lazy val gridButton       = uiScreen.findNiftyControl("gridButton",      classOf[Button])
  lazy val resourceButton   = uiScreen.findNiftyControl("resourceButton",  classOf[Button])
  lazy val layerUp          = uiScreen.findNiftyControl("layerUpButton",   classOf[Button])
  lazy val layerDown        = uiScreen.findNiftyControl("layerDownButton", classOf[Button])
  lazy val layerLabel       = uiScreen.findNiftyControl("layerLabel",      classOf[Label] )
  lazy val unitDescription  = uiScreen.findNiftyControl("unitDescription", classOf[Label] )
  lazy val endTurn          = uiScreen.findNiftyControl("endTurnButton",   classOf[Button])


  override def preStart() = {
    context.system.eventStream.subscribe( context.self, classOf[GameListenerMessage] )
    guiBus.subscribe( context.self, classOf[ButtonClickedEvent] )
    guiBus.subscribe( context.self, classOf[SelectionChanged]   )
    guiBus.subscribe( context.self, classOf[LevelChanged]       )
  }

  override def receive = {
    case m: GameListenerMessage => updateSelectedUnitStats()
    case m: SelectionChanged    => updateSelectedUnitStats()
    case LevelChanged(_, h)     => layerLabel.setText( LevelType.values()(h).getName )

    case b: ButtonClickedEvent if b.getButton == menu =>
      ui.gui.controller.guiMessages.publish( new PlainMessage("Main menu not yet implemented!") )
    case b: ButtonClickedEvent if b.getButton == messages =>
      ui.gui.gotoScreen( NoAiMessagesScreen )
    case b: ButtonClickedEvent if b.getButton == layerUp =>
      ui.gui.moveUp()
    case b: ButtonClickedEvent if b.getButton == layerDown =>
      ui.gui.moveDown()
    case b: ButtonClickedEvent if b.getButton == endTurn =>
      ui.gui.noai.endTurn()
    case b: ButtonClickedEvent if b.getButton == gridButton =>
      ui.gui.gridOn = !ui.gui.gridOn
    case b: ButtonClickedEvent if b.getButton == resourceButton =>
      ui.gui.resourcesOn = !ui.gui.resourcesOn
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
