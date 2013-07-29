package be.angelcorp.omicronai.agents

import akka.actor.Actor
import be.angelcorp.omicronai.{UnSupervisedMessage, SupervisorMessage, AiSupervisor}
import be.angelcorp.omicronai.Settings._
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

trait Agent extends Actor {
  private val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def act: Actor.Receive

  def receive: Actor.Receive = ({
    case UnSupervisedMessage(any) if act.isDefinedAt(any) =>
      act(any)
    case any if AiSupervisor.supervisor.isDefined && sender != AiSupervisor.supervisor.get && settings.ai.supervisor.forwardOnFor( any ) =>
      logger.trace(s"Forwarding command '$any' to supervisor")
      AiSupervisor.supervisor.get.forward( new SupervisorMessage( sender, self, any ) )
  }: Actor.Receive) orElse act

}

