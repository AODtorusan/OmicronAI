package be.angelcorp.omicron.noai.gui

import be.angelcorp.omicron.base.gui._
import de.lessvoid.nifty.Nifty

trait NiftyGuiInterface extends GuiInterface {

  def nifty: Nifty

  var hideGame: Boolean = false

  def gotoScreen(screen: GuiScreen) = {
    if (nifty.getCurrentScreen.getScreenId != screen.screenId) {
      screen.screenType match {
        case ScreenFill =>
          nifty.gotoScreen( screen.screenId )
          hideGame = true
        case ScreenOverlay =>
          hideGame = false
          nifty.gotoScreen( screen.screenId )
      }
    }
    nifty.getScreen( screen.screenId )
  }

}
