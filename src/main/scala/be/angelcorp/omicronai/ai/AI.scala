package be.angelcorp.omicronai.ai

import de.lessvoid.nifty.Nifty
import com.lyndir.omicron.api.model.{Color, PlayerKey, Player}
import com.lyndir.omicron.api.GameListener
import be.angelcorp.omicronai.gui.{AiGuiOverlay, AiGui, GuiInterface}

abstract class AI( playerId: Int, key: PlayerKey, name: String, primaryColor: Color, secondaryColor: Color  ) extends Player( playerId, key, name, primaryColor, secondaryColor) {

  def buildGuiInterface(gui: AiGuiOverlay, nifty: Nifty): GuiInterface

  def start() {
    getController.getGameController.setReady()
  }

}
