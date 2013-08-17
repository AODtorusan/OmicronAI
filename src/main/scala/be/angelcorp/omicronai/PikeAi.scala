package be.angelcorp.omicronai

import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.Color.Template._
import be.angelcorp.omicronai.agents.{Self, Admiral}
import be.angelcorp.omicronai.Settings.settings
import scala.concurrent.Await
import be.angelcorp.omicronai.agents.Self


class PikeAi( aiBuilder: (PikeAi, ActorSystem) => ActorRef, playerId: Int, key: PlayerKey, name: String, color: Color ) extends Player( playerId, key, name, color, color ) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit def timeout: Timeout = settings.ai.messageTimeout seconds;

  Security.authenticate(this, key)
  val actorSystem = ActorSystem()

  def this( aiBuilder: (PikeAi, ActorSystem) => ActorRef, builder: Game.Builder) =
    this( aiBuilder, builder.nextPlayerID, new PlayerKey, settings.ai.name, RED.get )

  lazy val admiralRef = {
    logger.info(s"Building AI logic for AI player ${getName}")
    aiBuilder(this, actorSystem)
  }
  lazy val admiral = {
    Await.result( admiralRef ? Self(), timeout.duration ).asInstanceOf[Admiral]
  }

}

