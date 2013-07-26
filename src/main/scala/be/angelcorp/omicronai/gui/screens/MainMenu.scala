package be.angelcorp.omicronai.gui.screens

import collection.JavaConverters._
import scala.concurrent.duration._
import akka.util.Timeout
import de.lessvoid.nifty.{NiftyEvent, NiftyEventSubscriber, Nifty}
import de.lessvoid.nifty.screen.{ScreenController, Screen}
import de.lessvoid.nifty.controls.{ButtonClickedEvent, ListBoxSelectionChangedEvent, ListBox}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.LevelType
import be.angelcorp.omicronai.gui.NiftyConstants._
import be.angelcorp.omicronai.gui.layerRender._
import be.angelcorp.omicronai.gui._
import scala.Some
import scala.io.Source
import java.io.ByteArrayInputStream
import scala.xml.Elem

object MainMenu extends GuiScreen {

  val name = "mainMenuScreen"

  def screen(nifty: Nifty, gui: AiGui) = {
    val xml =
      //<?xml version="1.0" encoding="UTF-8"?>
      <nifty xmlns="http://nifty-gui.lessvoid.com/nifty-gui" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >
        <screen id={name} controller={classOf[MainMenuController].getName}>
          <layer id="contentLayer" childLayout="vertical" backgroundColor={transparent}>
            <panel id="globalControls" backgroundColor={black(128)} align="right"
                   childLayout="vertical" width="25%" height="100%" paddingRight="5px">

              <effect>
                <onStartScreen name="move" mode="in"  direction="right" length="500" />
                <onEndScreen   name="move" mode="out" direction="right" length="500" />
              </effect>

              <control id="activeLayerList" align="center" name="listBox" vertical="off"      horizontal="off" displayItems="3" selectionMode="Single"   />
              <control id="layerList"       align="center" name="listBox" vertical="optional" horizontal="off" displayItems="4" selectionMode="Multiple" />
              <control id="actionList"      align="center" name="listBox" vertical="optional" horizontal="off" displayItems="4" selectionMode="Multiple" />

              <panel id="actionButtons" paddingRight="5px" childLayout="horizontal">
                <control id="acceptActionButton" name="button" label="Accept" />
                <control id="rejectActionButton" name="button" label="Reject" />
              </panel>

              <control id="exitButton" name="button" align="center" label="Exit" />

            </panel>
          </layer>
        </screen>
      </nifty>;

    loadNiftyXml( nifty, xml, new MainMenuController(gui) )
    nifty.getScreen( name )
  }

}

class MainMenuController(gui: AiGui) extends ScreenController with GuiSupervisorInterface {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit val timeout: Timeout = 5 seconds

  var nifty: Nifty = null
  val supervisor = gui.supervisor
  supervisor.listener = Some(this)

  lazy val actionList = nifty.getScreen(MainMenu.name).findNiftyControl("actionList", classOf[ListBox[WrappedAction]])

  override def onStartScreen() {}
  override def onEndScreen() {}

  override def bind(nifty: Nifty, screen: Screen) {
    this.nifty = nifty
  }

  def updateLayers(event: ListBoxSelectionChangedEvent[LayerRenderer]) {
    try{
      event.getListBox.getItems.asScala.zipWithIndex.foreach( entry => {
        val renderer = entry._1
        val index    = entry._2
        if ( event.getSelectionIndices.contains(index) ) {
          if (!gui.renderLayers.contains(renderer)) {
            logger.info("Enabling extra layer info: " + renderer.toString )
            gui.renderLayers.append( renderer )
          } else {
            logger.debug("Selected layer info " + renderer.toString + " was already enabled.")
          }
        } else {
          if (gui.renderLayers.contains(renderer)) {
            logger.info("Disabling extra layer info: " + renderer.toString )
            gui.renderLayers.remove( gui.renderLayers.indexOf(renderer) )
          } else {
            logger.trace("Deselected layer info " + renderer.toString + " was already disabled.")
          }
        }
      } )
    } catch {
      case e: Throwable => logger.info("Could not swap display layer, could not retrieve layer details; ", e)
    }
  }

  @NiftyEventSubscriber(pattern=".*List")
  def onListBoxSelectionChanged(id: String, event: ListBoxSelectionChangedEvent[_]) {
    id match {
      case "layerList"       => updateLayers( event.asInstanceOf[ListBoxSelectionChangedEvent[LayerRenderer]] )
      case "actionList"      =>
      case "activeLayerList" => updateActiveLayer( event.asInstanceOf[ListBoxSelectionChangedEvent[LevelType]] )
    }
  }

  def updateActiveLayer( event: ListBoxSelectionChangedEvent[LevelType] ) {
    event.getSelection.asScala.headOption match {
      case Some(newLayer) => gui.activeLayer = newLayer
      case _ =>
    }
  }

  def selectedAction = actionList.getSelection.asScala.headOption

  @NiftyEventSubscriber(id = "acceptActionButton")
  def acceptPressed(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => selectedAction match {
        case Some(wrappedAction) =>
          actionList.removeItem(wrappedAction)
          supervisor.acceptAction( wrappedAction )
        case None =>
      }
    case _ =>
  }

  @NiftyEventSubscriber(id = "rejectActionButton")
  def rejectPressed(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => selectedAction match {
      case Some(wrappedAction) =>
        actionList.removeItem(wrappedAction)
        supervisor.rejectAction( wrappedAction )
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

  def actionReceived(wrappedAction: WrappedAction) {
    actionList.addItem(wrappedAction)
  }

  def newTurn() {}

}
