package be.angelcorp.omicronai.gui.screens

import de.lessvoid.nifty.screen.Screen
import de.lessvoid.nifty.Nifty
import be.angelcorp.omicronai.PikeAi
import be.angelcorp.omicronai.gui.AiGui

trait GuiScreen {

  val name: String
  def screen(nifty: Nifty, gui: AiGui): Screen

}
