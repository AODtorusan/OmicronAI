package be.angelcorp.omicron.noai.gui.screens

import be.angelcorp.omicron.base.gui.nifty.NiftyConstants._
import be.angelcorp.omicron.base.gui.nifty.PopupController
import be.angelcorp.omicron.noai.gui.NoAiGui
import be.angelcorp.omicron.base.gui.{ScreenOverlay, ScreenType, GuiScreen}

object NoAiUserInterface extends GuiScreen {
  override val screenId   = "userInterface"
  override val screenType = ScreenOverlay
  def screen(noaiGui: NoAiGui) = {
    val xml =
    //<?xml version="1.0" encoding="UTF-8"?>
      <nifty xmlns="http://nifty-gui.lessvoid.com/nifty-gui" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >
        <useControls filename="nifty-default-controls.xml"/>
        <screen id={screenId} controller={classOf[NoAiUserInterfaceController].getName}>
          <layer id="contentLayer" childLayout="horizontal" backgroundColor={transparent}>

            <panel id="controlPanel" backgroundColor={black(200)} align="left" padding="5px" valign="bottom" childLayout="vertical" height="*" width="20%">
              <!-- <effect>
                <onStartScreen name="move" mode="in"  direction="left" length="1000" inherit="true" />
                <onEndScreen   name="move" mode="out" direction="left" length="1000" inherit="true" />
              </effect> -->
              <control id="menuButton" name="button" label="Menu" width="*" focusable="false" />

              <control name="label"  text=""  width="*" color={transparent} />

              <panel id="genericButtons" childLayout="horizontal" width="100%" >
                <control id="messagesButton" name="button" label="msg log" width="33%" focusable="false" />
                <control id="foo1"           name="label"  text="???"      width="*"   color={white} />
                <control id="foo2"           name="button" label="???"     width="33%" focusable="false" />
              </panel>

              <control name="label"  text=""  width="*" color={transparent} />

              <panel id="renderLayerControlPanel" childLayout="horizontal" width="100%" >
                <control id="layerUpButton"   name="button" label="up"     width="33%" focusable="false" />
                <control id="layerLabel"      name="label"  text="GROUND"  width="*"   color={white} />
                <control id="layerDownButton" name="button" label="down"   width="33%" focusable="false" />
              </panel>
              <panel id="layersControlPanel" childLayout="horizontal" width="100%" >
                <control id="gridButton"      name="button" label="grid"      width="33%" focusable="false" />
                <control id="resourceButton"  name="button" label="resources" width="*"   focusable="false" />
                <control id="errr2"           name="button" label="???"       width="33%" focusable="false" />
              </panel>

              <control name="label"  text=""  width="*" color={transparent} />

              <control id="unitDescription" textVAlign="top" textHAlign="left" name="label" text=""  height="*" width="*" color={white} />

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
    noaiGui.nifty.getScreen( screenId )
  }
}


