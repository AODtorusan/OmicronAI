package be.angelcorp.omicron.pike.gui

import scala.collection.mutable
import akka.actor.ActorRef
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.Nifty
import de.lessvoid.nifty.screen.{Screen, ScreenController}
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicron.base.gui.{GuiScreen, AiGuiOverlay}
import be.angelcorp.omicron.base.gui.layerRender.LayerRenderer
import be.angelcorp.omicron.base.gui.nifty.TreeBoxViewController
import be.angelcorp.omicron.base.gui.nifty.NiftyConstants._

object PikeUserInterface extends GuiScreen {
  val name = "userInterface"
  def screen(nifty: Nifty, gui: AiGuiOverlay) = {
    class ActorConverter extends TreeBoxViewController[ActorRef] {
      def stringify(item: ActorRef) = item.path.name
    }
    class ProbeConverter extends TreeBoxViewController[LayerRenderer] {
      val names = mutable.Map[ActorRef, String]()
      def stringify(item: LayerRenderer) = if (item == null) "null" else item.toString
    }

    val xml =
    //<?xml version="1.0" encoding="UTF-8"?>
      <nifty xmlns="http://nifty-gui.lessvoid.com/nifty-gui" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >
        <screen id={name} controller={classOf[PikeUserInterfaceScreenController].getName}>
          <layer id="contentLayer" childLayout="horizontal" backgroundColor={transparent}>

            <panel id="globalControls" backgroundColor={black(200)} valign="bottom" childLayout="horizontal" height="25%">

              <effect>
                <onStartScreen name="move" mode="in"  direction="bottom" length="1000" inherit="true" />
                <onEndScreen   name="move" mode="out" direction="bottom" length="1000" inherit="true" />
              </effect>

              <panel id="renderLayerPanel" childLayout="vertical" height="100%" width="180px" >
                <control id="layerList" name="listBox" vertical="optional" horizontal="off" displayItems="5" selectionMode="Multiple" />
                <panel id="renderLayerControlPanel" childLayout="horizontal" width="100%" >
                  <control id="layerUpButton"   name="button" label="up"     width="30%" />
                  <control id="layerLabel"      name="label"  text="GROUND" width="40%" color={white} />
                  <control id="layerDownButton" name="button" label="down"   width="30%" />
                </panel>
              </panel>

              <panel id="unitTreePanel" childLayout="vertical" height="100%" width="200px" >
                <control id="unitTree" name="treeBox" width="100%" vertical="on" horizontal="on" displayItems="4" selectionMode="Single"   viewConverterClass={classOf[ActorConverter].getName} />
                <control id="unitTreeUpdate" name="button" label="update unit tree" width="100%" />
              </panel>

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

                <control id="probeTab" name="tab" caption="Probe" childLayout="vertical" >
                  <control id="probesTree" name="treeBox" width="200px" vertical="optional" horizontal="optional" displayItems="4" selectionMode="Single" viewConverterClass={classOf[ProbeConverter].getName} />
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

    loadNiftyXml( nifty, xml, new PikeUserInterfaceScreenController(gui) )
    nifty.getScreen( name )
  }

}

class PikeUserInterfaceScreenController(gui: AiGuiOverlay) extends ScreenController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  var nifty: Nifty = null

  override def onStartScreen() {}
  override def onEndScreen() {}

  override def bind(nifty: Nifty, screen: Screen) {
    this.nifty = nifty
  }

}

