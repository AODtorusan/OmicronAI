package be.angelcorp.omicron.base.ai

import akka.actor.ActorSystem
import com.lyndir.omicron.api.model.{Color, PlayerKey, Game}

trait AIBuilder {

  def apply( actorSystem: ActorSystem, key: PlayerKey, builder: Game.Builder): AI

  def apply( actorSystem: ActorSystem, key: PlayerKey, name: String, color: Color, builder: Game.Builder): AI

}
