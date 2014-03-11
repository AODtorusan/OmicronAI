package be.angelcorp.omicron.noai.gui.screens

import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.{NiftyEvent, NiftyEventSubscriber, Nifty}
import de.lessvoid.nifty.screen.{Screen, ScreenController}
import de.lessvoid.nifty.controls.{ListBox, ButtonClickedEvent}
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.{UnitTypes, UnitType}
import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.base.ai.actions.ConstructionStartAction
import be.angelcorp.omicron.base.bridge.Asset
import be.angelcorp.omicron.base.gui.nifty.NiftyConstants._
import be.angelcorp.omicron.noai.gui.NoAiGui
import be.angelcorp.omicron.base.gui.{ScreenFill, ScreenType, GuiScreen}

object NoAiConstructionScreen extends GuiScreen {
  override val screenId   = "buildScreen"
  override val screenType = ScreenFill
  def screen(noaiGui: NoAiGui) = {
    val xml =
    //<?xml version="1.0" encoding="UTF-8"?>
      <nifty xmlns="http://nifty-gui.lessvoid.com/nifty-gui" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >
        <useControls filename="nifty-default-controls.xml"/>
        <screen id={screenId} controller={classOf[NoAiConstructionScreenController].getName}>
          <layer id="contentLayer" childLayout="vertical" backgroundColor={black}>
            <panel id="panels" childLayout="horizontal" width="*" height="*" >
              <panel id="leftPanel" childLayout="vertical" width="50%" height="*" padding="10px">
              </panel>
              <panel id="rightPanel" childLayout="vertical" width="50%" height="*" padding="10px">
                <control id="constructionTypeList" name="listBox" displayItems="21" vertical="on" horizontal="optional" width="*" height="*" selection="Single" />
              </panel>
            </panel>
            <panel id="bottom" childLayout="horizontal" width="*" height="40px" padding="10px">
              <control name="label" text="" width="10%"/>
              <control id="backButton"   name="button" label="Back" height="*" width="30%" />
              <control name="label" text="" width="20%"/>
              <control id="buildButton" name="button" label="Build" height="*" width="30%" />
              <control name="label" text="" width="10%"/>
            </panel>
          </layer>
        </screen>
      </nifty>;

    loadNiftyXml( noaiGui.nifty, xml, new NoAiConstructionScreenController(noaiGui) )
    noaiGui.nifty.getScreen( screenId )
  }
}

class NoAiConstructionScreenController(val gui: NoAiGui) extends ScreenController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  var nifty:               Nifty            = null
  var currentBuilder:      Option[Asset]    = None
  var currentDdestination: Option[Location] = None

  lazy val constructionScreen   = nifty.getScreen(NoAiConstructionScreen.screenId)
  lazy val constructionTypeList = constructionScreen.findNiftyControl("constructionTypeList", classOf[ListBox[UnitType]])

  override def onStartScreen() {
    for (i <- constructionTypeList.itemCount()-1 to 0 by -1 )
      constructionTypeList.removeItemByIndex(i)

    currentBuilder match {
      case Some(builder) =>
        // TODO: Correct getting of all possible units
        val possibleUnits = UnitTypes.values()

        val buildableModules = builder.constructors.map( _.getBuildsModule ).toList

        val buildableUnits = possibleUnits.filter( u => {
          val modules = u.createModules().asScala.map( _.getType )
          !modules.exists( mt => !buildableModules.contains(mt) )
        }  )
        for(ut <- buildableUnits)
          constructionTypeList.addItem( ut )
      case _ => logger.info("Opened construction menu, but no builder asset was bound!")
    }
  }
  override def onEndScreen() {
    currentBuilder      = None
    currentDdestination = None
  }

  override def bind(nifty: Nifty, screen: Screen) {
    this.nifty = nifty
  }

  def populate(builder: Asset, target: Location) {
    currentBuilder      = Some(builder)
    currentDdestination = Some(target)
  }

  @NiftyEventSubscriber(id = "backButton")
  def backButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => gui.gotoScreen( NoAiUserInterface )
    case _ =>
  }

  @NiftyEventSubscriber(id = "buildButton")
  def buildButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      if (currentBuilder.isDefined && currentDdestination.isDefined)
        constructionTypeList.getSelection.asScala.headOption match {
          case Some(typ) =>
            implicit val game = gui.noai.game
            gui.controller.updateOrConfirmAction( ConstructionStartAction(currentBuilder.get, currentDdestination.get, typ, gui.noai.world) )
            gui.gotoScreen( NoAiUserInterface )
          case _ =>
            logger.info(s"No construction type selected for ${currentBuilder.get} on ${currentDdestination.get}")
        }
      else gui.gotoScreen( NoAiUserInterface )
    case _ =>
  }

}