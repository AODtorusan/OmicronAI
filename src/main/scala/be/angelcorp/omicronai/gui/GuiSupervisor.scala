package be.angelcorp.omicronai.gui

import scala.collection.mutable
import akka.actor._
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.{UnSupervisedMessage, SupervisorMessage, AiSupervisor}
import be.angelcorp.omicronai.Settings.settings
import be.angelcorp.omicronai.ai.pike.PikeAi
import be.angelcorp.omicronai.ai.pike.agents.{ListMembers, Self, Name}

class GuiSupervisor(admiral: ActorRef, player: PikeAi, var listener: Option[GuiSupervisorInterface] = None) extends AiSupervisor {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  // [destination actor, message]
  private val messageBuffer = mutable.Map[ActorRef, mutable.ListBuffer[SupervisorMessage]]()
  // [actor, isActorOnAuto]
  private val onAuto        = mutable.Map[ActorRef, Boolean]()

  def isOnAuto( unit: ActorRef ) = onAuto.getOrElseUpdate(unit, settings.ai.supervisor.defaultAuto )

  def toggleAuto( unit: ActorRef ) {
    val newMode = !isOnAuto( unit )
    onAuto.update( unit, newMode )
    logger.debug(s"Toggled auto mode of unit $unit to: $newMode")
    if ( newMode ) {
      messageBuffer.get( unit ) match {
        case Some(list) =>
          logger.debug(s"Fast-forwarding all queued messages for unit $unit")
          list.foreach( m => {
            m.destination.forward( new UnSupervisedMessage(m.message) )
            listener match {
              case Some(l) => l.messageSend( m )
              case _ =>
            }
          } )
          list.clear()
        case None =>
      }
    }
  }

  def receive = {
    case SupervisorMessage(originalSender, originalReceiver, message) =>
      if (isOnAuto( originalReceiver )) {
        logger.trace(s"GuiSupervisor received message for $originalReceiver, but that unit is on auto, so passing along: $message")
        originalReceiver.forward(new UnSupervisedMessage(message))
      } else {
        logger.trace(s"GuiSupervisor intercepted message for $originalReceiver: $message")
        message match {
          case Name()        => originalReceiver.forward( new UnSupervisedMessage(message) )
          case Self()        => originalReceiver.forward( new UnSupervisedMessage(message) )
          case ListMembers() => originalReceiver.forward( new UnSupervisedMessage(message) )

          case m =>
            val bufferedMessage = SupervisorMessage(originalSender, originalReceiver, message)
            val list = messageBuffer.getOrElseUpdate( originalReceiver, mutable.ListBuffer[SupervisorMessage]() )
            list.append(bufferedMessage)

            listener match {
              case Some(l) => l.messageBuffered( bufferedMessage )
              case _ =>
            }
        }
      }
    case Self() => sender ! this

  }

  def acceptMessage(msg: SupervisorMessage) {
    // Remove from the message buffer if present
    messageBuffer.find( _._2 == msg) match {
      case Some( (key, _) ) => messageBuffer.remove( key )
      case None => logger.info(s"Accepted action from '$msg', but it this message was no longer present in the messageBuffer." )
    }

    msg.destination.tell( new UnSupervisedMessage( msg.message), msg.source )
  }

  def rejectMessage(msg: SupervisorMessage) {
    // Remove from the message buffer if present
    messageBuffer.find( _._2 == msg) match {
      case Some( (key, _) ) => messageBuffer.remove( key )
      case None => logger.info(s"Revoked action from '$msg', but it this message was no longer present in the messageBuffer." )
    }
  }

  def messagesFor(unit: ActorRef) = messageBuffer.getOrElse(unit, Nil).toSeq

}

trait GuiSupervisorInterface {

  def messageBuffered( msg: SupervisorMessage )
  def messageSend(     msg: SupervisorMessage )

}