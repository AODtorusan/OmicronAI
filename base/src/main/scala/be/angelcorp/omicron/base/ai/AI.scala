package be.angelcorp.omicron.base.ai

import akka.actor.ActorRef
import de.lessvoid.nifty.Nifty
import com.lyndir.omicron.api.model._
import be.angelcorp.omicron.base.gui.{GuiInterface, ActiveGameMode}
import be.angelcorp.omicron.base.Auth
import be.angelcorp.omicron.base.util.GenericEventBus

abstract class AI( playerId: Int, key: PlayerKey, name: String, primaryColor: Color, secondaryColor: Color  ) extends Player( playerId, key, name, primaryColor, secondaryColor) {

  def world: ActorRef

  def buildGuiInterface(gui: ActiveGameMode, guiBus: GenericEventBus, nifty: Nifty): GuiInterface

  /** The game has been build, prepare to start playing (game has been build, gui has not) */
  def prepare() {}

  /** Start playing the game (game and possible gui have been build) */
  def start() = auth {
    getController.getGameController.setReady()
  }

  protected val auth = new Auth( this, key )

  override val toString: String = s"<AI: $name>"

}
