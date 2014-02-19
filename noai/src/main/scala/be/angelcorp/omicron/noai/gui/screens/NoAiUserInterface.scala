package be.angelcorp.omicron.noai.gui.screens

import be.angelcorp.omicron.base.gui.nifty.NiftyConstants._
import be.angelcorp.omicron.base.gui.nifty.PopupController
import be.angelcorp.omicron.noai.gui.NoAiGui
import be.angelcorp.omicron.base.gui.GuiScreen

object NoAiUserInterface extends GuiScreen {
  val name = "userInterface"

  def screen(noaiGui: NoAiGui) = {
    val xml =
    //<?xml version="1.0" encoding="UTF-8"?>
      <nifty xmlns="http://nifty-gui.lessvoid.com/nifty-gui" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >
        <useControls filename="nifty-default-controls.xml"/>
        <screen id={name} controller={classOf[NoAiUserInterfaceController].getName}>
          <layer id="contentLayer" childLayout="horizontal" backgroundColor={transparent}>

            <panel id="controlPanel" backgroundColor={black(200)} align="left" valign="bottom" childLayout="vertical" height="*" width="20%">
              <!-- <effect>
                <onStartScreen name="move" mode="in"  direction="left" length="1000" inherit="true" />
                <onEndScreen   name="move" mode="out" direction="left" length="1000" inherit="true" />
              </effect> -->
              <control id="menuButton" name="button" label="Menu" width="*" focusable="false" />

              <control name="label"  text=""  width="*" color={transparent} />

              <panel id="renderLayerControlPanel" childLayout="horizontal" width="100%" >
                <control id="layerUpButton"   name="button" label="up"     width="30%" focusable="false" />
                <control id="layerLabel"      name="label"  text="GROUND"  width="40%" color={white} />
                <control id="layerDownButton" name="button" label="down"   width="30%" focusable="false" />
              </panel>
              <panel id="layersControlPanel" childLayout="horizontal" width="100%" >
                <control id="gridButton"      name="button" label="grid"      width="33%" focusable="false" />
                <control id="resourceButton"  name="button" label="resources" width="34%" focusable="false" />
                <control id="errr2"           name="button" label="???"       width="33%" focusable="false" />
              </panel>

              <control name="label"  text=""  width="*" color={transparent} />

              <control id="endTurnButton" name="button" label="End Turn" width="*" focusable="false" />
            </panel>

            <panel id="messagePanel" backgroundColor={black(200)} valign="bottom" childLayout="horizontal" height="25%">
              <!-- <effect>
                <onStartScreen name="move" mode="in"  direction="bottom" length="1000" inherit="true" />
                <onEndScreen   name="move" mode="out" direction="bottom" length="1000" inherit="true" />
              </effect> -->
              <control id="messages" name="label" width="100%" height="100%" text="hello textfield" textHAlign="left" textVAlign="top" wrap="on" />
            </panel>
          </layer>
        </screen>
        {PopupController.xml[NoAiPopupController]}
      </nifty>;

    loadNiftyXml( noaiGui.nifty, xml, new NoAiUserInterfaceController(noaiGui) )
    noaiGui.nifty.getScreen( name )
  }

}


