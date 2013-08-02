package be.angelcorp.omicronai.gui.screens

import collection.mutable
import collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import de.lessvoid.nifty.{NiftyEvent, NiftyEventSubscriber, Nifty}
import de.lessvoid.nifty.screen.{ScreenController, Screen}
import de.lessvoid.nifty.controls._
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.LevelType
import be.angelcorp.omicronai.gui.NiftyConstants._
import be.angelcorp.omicronai.gui.layerRender._
import be.angelcorp.omicronai.gui._
import scala.Some
import be.angelcorp.omicronai.gui.nifty.{TreeBoxViewController, ListBoxViewConverter}
import akka.actor.ActorRef
import scala.concurrent.Await
import be.angelcorp.omicronai.agents.{GetAsset, ValidateAction, Name}
import be.angelcorp.omicronai.SupervisorMessage
import scala.Some
import be.angelcorp.omicronai.agents.ValidateAction
import be.angelcorp.omicronai.agents.GetAsset
import be.angelcorp.omicronai.agents.Name
import be.angelcorp.omicronai.SupervisorMessage
import be.angelcorp.omicronai.assets.Asset

object MainMenu extends GuiScreen {

  val name = "mainMenuScreen"

  def screen(nifty: Nifty, gui: AiGui) = {
    class ActorConverter extends TreeBoxViewController[ActorRef] {
      implicit val timeout: Timeout = 5 seconds;
      val names = mutable.Map[ActorRef, String]()

      def stringify(item: ActorRef) = names.getOrElseUpdate(item, {
        Await.result( ask(item, Name()), timeout.duration).asInstanceOf[String]
      })
    }

    val xml =
      //<?xml version="1.0" encoding="UTF-8"?>
      <nifty xmlns="http://nifty-gui.lessvoid.com/nifty-gui" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >
        <screen id={name} controller={classOf[MainMenuController].getName}>
          <layer id="contentLayer" childLayout="horizontal" backgroundColor={transparent}>

            <panel id="globalControls" backgroundColor={black(200)} valign="bottom" childLayout="horizontal" height="25%">

              <effect>
                <onStartScreen name="move" mode="in"  direction="bottom" length="1000" inherit="true" />
                <onEndScreen   name="move" mode="out" direction="bottom" length="1000" inherit="true" />
              </effect>

              <!--<control id="activeLayerList" name="listBox" vertical="off"      horizontal="off" displayItems="3" selectionMode="Single"   />-->
              <panel id="renderLayerPanel" childLayout="vertical" height="100%" width="180px" >
                <control id="layerList" name="listBox" vertical="optional" horizontal="off" displayItems="5" selectionMode="Multiple" />
                <panel id="renderLayerControlPanel" childLayout="horizontal" width="100%" >
                  <control id="layerUpButton"   name="button" label="up"     width="30%" />
                  <control id="layerLabel"      name="label"  text="GROUND" width="40%" color={white} />
                  <control id="layerDownButton" name="button" label="down"   width="30%" />
                </panel>
              </panel>

              <control id="unitTree" name="treeBox" width="200px" vertical="on" horizontal="optional" displayItems="5" selectionMode="Single"   viewConverterClass={classOf[ActorConverter].getName} />

              <control id="controlTabs" name="tabGroup" caption="Control" >

                <control id="messageTab" name="tab" caption="Messages" childLayout="horizontal" >
                  <control id="messageList" align="center" name="listBox" vertical="optional" horizontal="optional" displayItems="4" selectionMode="Single" />
                  <panel id="actionButtons" childLayout="vertical" width="50px" paddingLeft="5px" >
                    <control id="acceptButton" name="button" label="Accept" width="50px" />
                    <control id="rejectButton" name="button" label="Reject" width="50px" />
                    <control id="modifyButton" name="button" label="Modify" width="50px" />
                    <control id="addButton"    name="button" label="Add"    width="50px" />
                  </panel>
                </control>

                <control id="goalTab" name="tab" caption="Goals" childLayout="vertical" >
                  <control id="goalList" align="center" name="listBox" vertical="optional" horizontal="off" displayItems="4" selectionMode="Single"   />
                </control>

              </control>

              <panel id="actionButtons" childLayout="vertical" height="100%" >
                <control id="autoButton"   name="button" label="Auto"   />
                <control id="centerButton" name="button" label="Center" />
                <control id="dummy1Button" name="button" label="-"      />
                <control id="dummy2Button" name="button" label="-"      />
                <control id="exitButton"   name="button" label="Exit"   />
              </panel>

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

  lazy val layerUpButton   = nifty.getScreen(MainMenu.name).findNiftyControl("layerUpButton",   classOf[Button])
  lazy val layerLabel      = nifty.getScreen(MainMenu.name).findNiftyControl("layerLabel",      classOf[Label])
  lazy val layerDownButton = nifty.getScreen(MainMenu.name).findNiftyControl("layerDownButton", classOf[Button])

  lazy val messageList = nifty.getScreen(MainMenu.name).findNiftyControl("messageList", classOf[ListBox[SupervisorMessage]])
  lazy val unitTree    = nifty.getScreen(MainMenu.name).findNiftyControl("unitTree",   classOf[TreeBox[ActorRef]])

  lazy val autoButton   = nifty.getScreen(MainMenu.name).findNiftyControl("autoButton",   classOf[Button])
  lazy val centerButton = nifty.getScreen(MainMenu.name).findNiftyControl("centerButton", classOf[Button])

  lazy val acceptButton = nifty.getScreen(MainMenu.name).findNiftyControl("acceptButton", classOf[Button])
  lazy val rejectButton = nifty.getScreen(MainMenu.name).findNiftyControl("rejectButton", classOf[Button])
  lazy val modifyButton = nifty.getScreen(MainMenu.name).findNiftyControl("modifyButton", classOf[Button])
  lazy val addButton    = nifty.getScreen(MainMenu.name).findNiftyControl("addButton",    classOf[Button])

  override def onStartScreen() {}
  override def onEndScreen() {}

  override def bind(nifty: Nifty, screen: Screen) {
    this.nifty = nifty
  }

  @NiftyEventSubscriber(id = "layerUpButton")
  def layerUpButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      gui.view.activeLayer = math.min( gui.view.activeLayer + 1, LevelType.values().size - 1 )
      layerLabel.setText( LevelType.values()(gui.view.activeLayer).getName )
    case _ =>
  }

  @NiftyEventSubscriber(id = "layerDownButton")
  def layerDownButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      gui.view.activeLayer = math.max( gui.view.activeLayer - 1, 0 )
      layerLabel.setText( LevelType.values()(gui.view.activeLayer).getName )
    case _ =>
  }

  @NiftyEventSubscriber(id = "layerList")
  def updateLayers(id: String, event: ListBoxSelectionChangedEvent[LayerRenderer]) {
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

  @NiftyEventSubscriber(id = "activeLayerList")
  def updateActiveLayer( id: String, event: ListBoxSelectionChangedEvent[LevelType] ) {
    event.getSelection.asScala.headOption match {
      case Some(newLayer) => gui.view.activeLayer = newLayer
      case _ =>
    }
  }

  @NiftyEventSubscriber(id = "unitTree")
  def updateUnitSelection( id: String, event: TreeItemSelectionChangedEvent[ActorRef] ) {
    selectedUnit match {
      case Some(unit) => updateUnitUi()
      case None =>
    }
  }

  def selectedMessage = messageList.getSelection.asScala.headOption
  def selectedUnit    = unitTree.getSelection.asScala.headOption.map( _.getValue )

  @NiftyEventSubscriber(id = "acceptButton")
  def acceptButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => selectedMessage match {
        case Some(message) =>
          messageList.removeItem(message)
          supervisor.acceptMessage( message )
        case None =>
      }
    case _ =>
  }

  @NiftyEventSubscriber(id = "rejectButton")
  def rejectButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent => selectedMessage match {
      case Some(message) =>
        messageList.removeItem(message)
        supervisor.rejectMessage(message)
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

  @NiftyEventSubscriber(id = "exitButton")
  def exit(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      nifty.exit()
      System.exit(0)
    case _ =>
  }

  @NiftyEventSubscriber(id = "autoButton")
  def autoButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      selectedUnit match {
        case Some(unit) =>
          supervisor.toggleAuto( unit )
          updateUnitUi()
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
            implicit val timeout: Timeout = 5 seconds;
            val asset = Await.result( ask(unit, GetAsset()), timeout.duration).asInstanceOf[Asset]
            gui.view.centerOn( asset.location )
          } catch {
            case e: Throwable => logger.info(s"Cannot center on unit ($unit), it does not have an asset or was not received in time!")
          }
        case None =>
      }
    case _ =>
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

  def updateUnitUi() {
    messageList.clear()
    selectedUnit match {
      case Some(unit) =>
        enable(autoButton)
        autoButton.setText( "Auto " + (if (supervisor.isOnAuto( unit )) "(on)" else "(off)") )
        enable(centerButton)
        enable(acceptButton)
        enable(rejectButton)
        enable(modifyButton)
        messageList.addAllItems( supervisor.messagesFor(unit).asJava )
      case _ =>
        disable(autoButton)
        disable(centerButton)
        disable(acceptButton)
        disable(rejectButton)
        disable(modifyButton)
        disable(addButton)
    }
  }

  def disable(control: NiftyControl) {
    control.setEnabled(false)
    control.setFocusable(false)
  }
  def enable(control: NiftyControl) {
    control.setEnabled(true)
    control.setFocusable(true)
  }

}
