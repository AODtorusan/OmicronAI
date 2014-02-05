package be.angelcorp.omicronai.ai.pike.agents

import akka.actor.Actor
import be.angelcorp.omicronai.{UnSupervisedMessage, SupervisorMessage, AiSupervisor}
import be.angelcorp.omicronai.configuration.Configuration
import Configuration._
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

trait Agent extends Actor {
  private val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def act: Actor.Receive

  def name = self.path.name

  def receive: Actor.Receive = ({
    case UnSupervisedMessage(Self()) | Self() =>
      logger.trace(s"This handle of $name to $sender")
      sender ! this
    case UnSupervisedMessage(ListMembers()) | ListMembers() =>
      logger.trace(s"Children of $name to $sender (${context.children}})")
      sender ! context.children

    case UnSupervisedMessage(any) if act.isDefinedAt(any) =>
      act(any)
    case any if AiSupervisor.supervisor.isDefined && sender != AiSupervisor.supervisor.get && config.ai.supervisor.forwardOnFor( any ) =>
      logger.trace(s"Forwarding command '$any' to supervisor")
      AiSupervisor.supervisor.get.forward( new SupervisorMessage( sender, self, any ) )
  }: Actor.Receive) orElse act

  override def unhandled(message: Any) {
    logger.warn(s"Unhandled message for unit $name ($self): $message")
  }

}

