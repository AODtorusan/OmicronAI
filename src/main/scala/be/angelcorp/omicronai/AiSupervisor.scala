package be.angelcorp.omicronai

import akka.actor.{ActorRef, Actor}

trait AiSupervisor extends Actor

object AiSupervisor {

  var supervisor: Option[ActorRef] = None

}
