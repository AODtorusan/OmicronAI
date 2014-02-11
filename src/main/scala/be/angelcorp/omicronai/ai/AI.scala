package be.angelcorp.omicronai.ai

import de.lessvoid.nifty.Nifty
import com.lyndir.omicron.api.model._
import be.angelcorp.omicronai.gui.{AiGuiOverlay, GuiInterface}

abstract class AI( playerId: Int, key: PlayerKey, name: String, primaryColor: Color, secondaryColor: Color  ) extends Player( playerId, key, name, primaryColor, secondaryColor) {

  def buildGuiInterface(gui: AiGuiOverlay, nifty: Nifty): GuiInterface

  /** The game has been build, prepare to start playing (game has been build, gui has not) */
  def prepare() {}

  /** Start playing the game (game and possible gui have been build) */
  def start() {
    Security.authenticate(this, key)
    getController.getGameController.setReady()
  }

}
