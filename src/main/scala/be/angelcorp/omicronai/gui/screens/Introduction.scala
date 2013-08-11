package be.angelcorp.omicronai.gui.screens

import be.angelcorp.omicronai.gui.NiftyConstants._
import de.lessvoid.nifty.screen.DefaultScreenController
import de.lessvoid.nifty.Nifty
import be.angelcorp.omicronai.gui.AiGui
import de.lessvoid.nifty.builder.{LayerBuilder, ScreenBuilder}
import be.angelcorp.omicronai.gui.effects.FadeEffectBuilder
import be.angelcorp.omicronai.gui.screens.ui.UserInterface

object Introduction extends GuiScreen {

  val name = "introductionScreen"

  def screen(nifty: Nifty, gui: AiGui) = {
    val controller_ = new DefaultScreenController{
      override def onStartScreen() { gotoScreen( UserInterface.name ) }
    }

    val xml =
      //<?xml version="1.0" encoding="UTF-8"?>
      <nifty xmlns="http://nifty-gui.lessvoid.com/nifty-gui" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >
        <screen id={name} controller={controller_.getClass.getName}>
          <layer id="background" backgroundColor={transparent} >
          </layer>
        </screen>
      </nifty>;

    loadNiftyXml( nifty, xml, controller_ )
    nifty.getScreen( name )
  }
}
