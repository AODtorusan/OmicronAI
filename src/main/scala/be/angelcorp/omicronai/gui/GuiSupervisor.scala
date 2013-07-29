package be.angelcorp.omicronai.gui

import collection.mutable
import concurrent.duration._
import concurrent.Await
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.{UnSupervisedMessage, SupervisorMessage, PikeAi, AiSupervisor}
import be.angelcorp.omicronai.actions.Action
import be.angelcorp.omicronai.Settings.settings

import be.angelcorp.omicronai.agents._

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
          case NewTurn() => originalReceiver.forward( new UnSupervisedMessage(message) )
          case Name()    => originalReceiver.forward( new UnSupervisedMessage(message) )
          case Self()    => originalReceiver.forward( new UnSupervisedMessage(message) )

          case m: ValidateAction =>
            val bufferedMessage = SupervisorMessage(originalSender, originalReceiver, message)
            val list = messageBuffer.getOrElseUpdate( originalReceiver, mutable.ListBuffer[SupervisorMessage]() )
            list.append(bufferedMessage)

            listener match {
              case Some(l) => l.messageBuffered( bufferedMessage )
              case _ =>
            }

          case any =>
            logger.warn(s"GuiSupervisor received unknown message '$any', forwarding to the original receiver")
            originalReceiver.forward(message)
        }
      }
    case Self() => sender ! this

  }

  def acceptAction(msg: SupervisorMessage) {
    msg.message match {
      case ValidateAction(action, actor) =>
        // Remove from the message buffer if present
        messageBuffer.find( _._2 == msg) match {
          case Some( (key, _) ) => messageBuffer.remove( key )
          case None => logger.info(s"Accepted action from '$msg', but it this message was no longer present in the messageBuffer." )
        }
        actor ! new UnSupervisedMessage( ExecuteAction( action ) )
      case _ => logger.warn(s"Tried to accept an action from the $msg, but this is not a ValidateAction command!")
    }
  }

  def rejectAction(msg: SupervisorMessage) {
    msg.message match {
      case ValidateAction(action, actor) =>
        // Remove from the message buffer if present
        messageBuffer.find( _._2 == msg) match {
          case Some( (key, _) ) => messageBuffer.remove( key )
          case None => logger.info(s"Revoked action from '$msg', but it this message was no longer present in the messageBuffer." )
        }
        actor ! new UnSupervisedMessage( RevokeAction( action ) )
      case _ => logger.warn(s"Tried to revoke an action from the $msg, but this is not a ValidateAction command!")
    }
  }

}

trait GuiSupervisorInterface {

  def messageBuffered( msg: SupervisorMessage )
  def messageSend(     msg: SupervisorMessage )

}