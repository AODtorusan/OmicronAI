package be.angelcorp.omicronai.actions

import com.lyndir.omicron.api.model.Player
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.agents.Soldier
import be.angelcorp.omicronai.MetaData
import akka.actor.ActorRef

trait Action {

  /**
   * Unit to which the action belongs
   */
  def unit: ActorRef

  /**
   * Let the unit execute this action
   * @param aiPlayer Key that identifies the acting player (owner of the unit)
   * @return True if successfull
   */
  def performAction(aiPlayer: Player): Boolean

  /**
   * Metadata surrounding the action
   */
  def metadata: Seq[MetaData]

}
