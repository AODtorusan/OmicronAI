package be.angelcorp.omicronai.actions

import com.lyndir.omicron.api.model.Player
import be.angelcorp.omicronai.agents.Soldier
import be.angelcorp.omicronai.MetaData
import akka.actor.ActorRef

case class NoAction() extends Action {

  def unit: ActorRef = ActorRef.noSender

  def performAction(aiPlayer: Player): Boolean = true

  def metadata: Seq[MetaData] = Nil

  override val toString = "no action"

}
