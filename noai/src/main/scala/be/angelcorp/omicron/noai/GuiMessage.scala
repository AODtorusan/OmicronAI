package be.angelcorp.omicron.noai

import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.noai.gui.NoAiGui
import be.angelcorp.omicron.noai.gui.screens.NoAiUserInterface

trait GuiMessage {

  def message: String

  def onClick( gui: NoAiGui )

}

case class PlainMessage(message: String) extends GuiMessage {
  override def onClick(gui: NoAiGui) {}
}

case class LocatedMessage(message: String, location: Location) extends GuiMessage {

  override def onClick( gui: NoAiGui ) {
    gui.gotoScreen( NoAiUserInterface )
    gui.frame.view.centerOn( location )
  }

}
