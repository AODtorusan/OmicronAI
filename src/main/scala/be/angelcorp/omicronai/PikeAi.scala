package be.angelcorp.omicronai

import com.lyndir.omnicron.api.model.{PlayerKey, Player, Game}
import com.lyndir.omnicron.api.controller.GameController
import com.lyndir.omnicron.api.model.Color.Template._
import be.angelcorp.omicronai.agents.{NewTurn, Admiral}
import akka.actor.{Props, ActorSystem, Actor}
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

class PikeAi extends Actor {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  logger.info("Building new game")
  val builder = Game.builder
  val pike = new Player( builder.nextPlayerID, new PlayerKey, "PikeAI", RED.get, RED.get )
  builder.getPlayers.add(pike)
  val gameController = new GameController( builder.build )

  override def preStart() = {

    logger.info("Recruiting admiral Pike")
    val commander = context.actorOf(Props(new Admiral(pike)), name = "AdmiralPike")

    commander ! NewTurn()
  }

  def receive = {
    case any => throw new RuntimeException(s"PikeAI cannot receive any messages, but received the following: $any")
  }

}

object PikeAi extends App {

  val system = ActorSystem( "PikeAi" )
  val myActor = system.actorOf(Props[PikeAi], name = "PikeAi")

}
