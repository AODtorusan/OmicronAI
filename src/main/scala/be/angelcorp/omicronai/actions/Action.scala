package be.angelcorp.omicronai.actions

import com.lyndir.omicron.api.model.Player
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.agents.Soldier

trait Action {

  def performAction(aiPlayer: Player, soldier: Soldier): Boolean

}
