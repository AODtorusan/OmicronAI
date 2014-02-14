package be.angelcorp.omicronai.ai

import akka.actor.ActorRef
import de.lessvoid.nifty.Nifty
import com.lyndir.omicron.api.model._
import be.angelcorp.omicronai.gui.{AiGuiOverlay, GuiInterface}

abstract class AI( playerId: Int, key: PlayerKey, name: String, primaryColor: Color, secondaryColor: Color  ) extends Player( playerId, key, name, primaryColor, secondaryColor) {

  def world: ActorRef

  def buildGuiInterface(gui: AiGuiOverlay, nifty: Nifty): GuiInterface

  /** The game has been build, prepare to start playing (game has been build, gui has not) */
  def prepare() {}

  /** Start playing the game (game and possible gui have been build) */
  def start() = withSecurity(key) {
    getController.getGameController.setReady()
  }

  def withSecurity[T](key: PlayerKey)( body: => T ) =
    if (Security.isAuthenticatedAs(this)) {
      body
    } else {
      Security.authenticate(this, key)
      val result = body
      Security.invalidate()
      result
    }

  protected[ai] def getKey = key

}
