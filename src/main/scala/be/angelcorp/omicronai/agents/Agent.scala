package be.angelcorp.omicronai.agents

import akka.actor.Actor
import org.slf4j.LoggerFactory

abstract class Agent extends Actor

class DeafAgent extends Agent {
  val logger = LoggerFactory.getLogger( classOf[Admiral] )

  def receive = {
    case event => logger.warn("Dude what the hell are you trying to tell me, I don't get this: {}", event)
  }

}
