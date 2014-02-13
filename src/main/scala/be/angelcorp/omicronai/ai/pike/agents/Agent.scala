package be.angelcorp.omicronai.ai.pike.agents

import akka.actor.{ActorRef, Actor}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.{Security, Game}
import be.angelcorp.omicronai.{UnSupervisedMessage, SupervisorMessage, AiSupervisor}
import be.angelcorp.omicronai.ai.AI
import be.angelcorp.omicronai.configuration.Configuration._
import be.angelcorp.omicronai.ai.actions.ActionExecutor
import scala.util.Failure

trait Agent extends Actor {
  private val logger = Logger( LoggerFactory.getLogger( getClass ) )

  protected def aiExec: ActionExecutor
  protected def ai:     AI

  def act:    Actor.Receive

  def name = self.path.name

  def receive: Actor.Receive = ({
    case UnSupervisedMessage(Self()) | Self() =>
      logger.trace(s"This handle of $name to $sender")
      sender ! this
    case UnSupervisedMessage(ListMembers()) | ListMembers() =>
      logger.trace(s"Children of $name to $sender (${context.children}})")
      sender ! context.children

    case UnSupervisedMessage(any) =>
      act(any)
    case any if AiSupervisor.supervisor.isDefined && sender != AiSupervisor.supervisor.get && config.ai.supervisor.isMessagesSupervised( any ) =>
      logger.trace(s"Forwarding command '$any' to supervisor")
      AiSupervisor.supervisor.get.forward( new SupervisorMessage( sender, self, any ) )
  }: Actor.Receive) orElse act

  override def unhandled(message: Any) {
    message match {
      case Failure( e ) =>
        logger.error(s"Unhandled failure message for unit $name ($self)", e)
      case akka.actor.Status.Failure( e ) =>
        logger.error(s"Unhandled failure message for unit $name ($self)", e)
      case e: Throwable =>
        logger.error(s"Unhandled exception message for unit $name ($self)", e)
      case m =>
        logger.warn(s"Unhandled message for unit $name ($self): $m")
    }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    logger.warn(s"Agent crashed ${if (message.isDefined) "on " + message.get.toString}", reason)
    super.preRestart(reason, message)
  }

  def withSecurity[T]( body: => T ) =
    ai.withSecurity(ai.getKey)(body)

}

