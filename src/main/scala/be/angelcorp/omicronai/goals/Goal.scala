package be.angelcorp.omicronai.goals

import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.agents.Squad
import be.angelcorp.omicronai.actions.Action
import akka.actor.ActorRef
import be.angelcorp.omicronai.information.InformationLayer

trait Goal {

  def findActions[T]( unit: Map[ActorRef, Option[Asset]] ): Map[ActorRef, Seq[Action]]
  def informationLayers: Seq[InformationLayer]

}

class NoGoal extends Goal {
  def findActions[T]( units: Map[ActorRef, Option[Asset]] ) = units.mapValues( asset => Nil )
  def informationLayers: Seq[InformationLayer] = Nil
}
