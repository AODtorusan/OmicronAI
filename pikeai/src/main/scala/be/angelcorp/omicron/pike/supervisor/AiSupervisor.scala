package be.angelcorp.omicron.pike.supervisor

import akka.actor.{ActorRef, Actor}

trait AiSupervisor extends Actor

object AiSupervisor {

  var supervisor: Option[ActorRef] = None

}

case class UnSupervisedMessage(message: Any)
case class SupervisorMessage( source: ActorRef, destination: ActorRef, message: Any) {
  override lazy val toString = s"SupervisorMessage from $source to $destination ($message)"
}
