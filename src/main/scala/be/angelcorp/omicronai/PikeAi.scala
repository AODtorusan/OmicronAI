package be.angelcorp.omicronai

import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import com.lyndir.omicron.api.model.{Color, PlayerKey, Player, Game}
import com.lyndir.omicron.api.model.Color.Template._
import be.angelcorp.omicronai.agents.{Self, Admiral}
import be.angelcorp.omicronai.Settings.settings
import scala.concurrent.Await


class PikeAi( aiBuilder: (PikeAi, ActorSystem) => ActorRef, playerId: Int, key: PlayerKey, name: String, color: Color ) extends Player( playerId, key, name, color, color ) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val actorSystem = ActorSystem()

  def this( aiBuilder: (PikeAi, ActorSystem) => ActorRef, builder: Game.Builder) =
    this( aiBuilder, builder.nextPlayerID, new PlayerKey, settings.ai.name, RED.get )

  lazy val admiralRef = {
    logger.info(s"Building AI logic for AI player ${getName}")
    aiBuilder(this, actorSystem)
  }

  override lazy val getController = {
    implicit val timeout: Timeout = 5 seconds;
    Await.result( ask(admiralRef, Self()), timeout.duration ).asInstanceOf[Admiral]
  }

}

