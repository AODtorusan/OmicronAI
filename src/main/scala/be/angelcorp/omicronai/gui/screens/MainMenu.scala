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
import be.angelcorp.omicronai.assets.Asset
import scala.Some
import be.angelcorp.omicronai.agents.ValidateAction
import be.angelcorp.omicronai.agents.GetAsset
import be.angelcorp.omicronai.agents.Name
import be.angelcorp.omicronai.SupervisorMessage

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
          <layer id="contentLayer" childLayout="vertical" backgroundColor={transparent}>

            <panel id="globalControls" backgroundColor={black(128)} align="right"
                   childLayout="vertical" width="25%" height="100%" >

              <effect>
                <onStartScreen name="move" mode="in"  direction="right" length="1000" inherit="true" />
                <onEndScreen   name="move" mode="out" direction="right" length="1000" inherit="true" />
              </effect>

              <control id="activeLayerList" align="center" name="listBox" vertical="off"      horizontal="off" displayItems="3" selectionMode="Single"   />
              <control id="layerList"       align="center" name="listBox" vertical="optional" horizontal="off" displayItems="4" selectionMode="Multiple" />
              <control id="unitTree"        align="center" name="treeBox" vertical="on"       horizontal="off" displayItems="4" selectionMode="Single"   viewConverterClass={classOf[ActorConverter].getName} />

              <panel id="actionButtons" childLayout="absolute" height="40px" >
                <control id="autoButton"   name="button" label="Auto"   x="00%" y="00px" width="50%" height="20px" />
                <control id="centerButton" name="button" label="Center" x="50%" y="00px" width="50%" height="20px" />
                <control id="dummy1Button" name="button" label="-"      x="00%" y="20px" width="50%" height="20px" />
                <control id="dummy2Button" name="button" label="-"      x="50%" y="20px" width="50%" height="20px" />
              </panel>

              <control id="controlTabs" name="tabGroup" caption="Control" >

                <control id="actionTab" name="tab" caption="Actions" childLayout="vertical" >
                  <control id="actionList" align="center" name="listBox" vertical="optional" horizontal="off" displayItems="4" selectionMode="Single"   />
                  <panel id="actionButtons" childLayout="horizontal">
                    <control id="acceptActionButton" name="button" label="Accept" width="50%" />
                    <control id="rejectActionButton" name="button" label="Reject" width="50%" />
                  </panel>
                </control>

                <control id="goalTab" name="tab" caption="Goals" childLayout="vertical" >
                  <control id="goalList" align="center" name="listBox" vertical="optional" horizontal="off" displayItems="4" selectionMode="Single"   />
                </control>

              </control>





              <control id="exitButton" name="button" align="center" label="Exit" width="100%" />

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

  lazy val actionList = nifty.getScreen(MainMenu.name).findNiftyControl("actionList", classOf[ListBox[SupervisorMessage]])
  lazy val unitTree   = nifty.getScreen(MainMenu.name).findNiftyControl("unitTree",   classOf[TreeBox[ActorRef]])

  lazy val autoButton   = nifty.getScreen(MainMenu.name).findNiftyControl("autoButton",   classOf[Button])
  lazy val centerButton = nifty.getScreen(MainMenu.name).findNiftyControl("centerButton", classOf[Button])

  override def onStartScreen() {}
  override def onEndScreen() {}

  override def bind(nifty: Nifty, screen: Screen) {
    this.nifty = nifty
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
      case Some(unit) =>
        autoButton.setText( "Auto " + (if (supervisor.isOnAuto( unit )) "(on)" else "(off)") )
      case None =>
    }
  }

  def selectedAction = actionList.getSelection.asScala.headOption
  def selectedUnit   = unitTree.getSelection.asScala.headOption.map( _.getValue )

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

  @NiftyEventSubscriber(id = "autoButton")
  def autoButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      selectedUnit match {
        case Some(unit) =>
          supervisor.toggleAuto( unit )
          autoButton.setText( "Auto " + (if (supervisor.isOnAuto( unit )) "(on)" else "(off)") )
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

  def messageBuffered( msg: SupervisorMessage ) = msg.message match {
    case ValidateAction(a, s) => actionList.addItem( msg )
  }

  def messageSend(msg: SupervisorMessage) = msg.message match {
    case ValidateAction(a, s) => actionList.removeItem( msg )
  }

}
