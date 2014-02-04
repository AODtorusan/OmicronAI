package be.angelcorp.omicronai.ai.noai.gui.screens

import be.angelcorp.omicronai.ai.noai.gui.NoAiGui
import be.angelcorp.omicronai.gui.nifty.NiftyConstants
import NiftyConstants._
import be.angelcorp.omicronai.gui.screens.GuiScreen
import be.angelcorp.omicronai.gui.screens.ui.UserInterface

object NoAiUserInterface extends GuiScreen {
  val name = UserInterface.name

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
                <control id="gridButton"      name="button" label="grid"  width="33%" focusable="false" />
                <control id="errr"            name="button" label="???"   width="34%" focusable="false" />
                <control id="errr2"           name="button" label="???"   width="33%" focusable="false" />
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
        <popup id="niftyPopupMenu" childLayout="absolute-inside" controller={classOf[NoAiPopupController].getName} width="100px">
          <interact onClick="closePopup()" onSecondaryClick="closePopup()" onTertiaryClick="closePopup()" />
          <control id="#menu" name="niftyMenu" />
        </popup>
      </nifty>;

    loadNiftyXml( noaiGui.nifty, xml, new NoAiUserInterfaceController(noaiGui) )
    noaiGui.nifty.getScreen( name )
  }

}


