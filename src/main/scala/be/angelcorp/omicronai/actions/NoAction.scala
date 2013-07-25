package be.angelcorp.omicronai.actions

import com.lyndir.omicron.api.model.Player
import be.angelcorp.omicronai.agents.Soldier

case class NoAction() extends Action {

  def performAction(aiPlayer: Player, soldier: Soldier) = true
  override val toString = "no action"

}
