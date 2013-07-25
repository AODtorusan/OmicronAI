package be.angelcorp.omicronai.gui

import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import be.angelcorp.omicronai.{PikeAi, AiSupervisor}
import akka.actor.ActorRef
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.collection.mutable.ListBuffer
import be.angelcorp.omicronai.actions.Action
import akka.util.Timeout
import scala.concurrent.Await
import be.angelcorp.omicronai.agents._

class GuiSupervisor(admiral: ActorRef, player: PikeAi, var listener: Option[GuiSupervisorInterface] = None) extends AiSupervisor {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val actionQue = ListBuffer[WrappedAction]()

  def receive = {
    case NewTurn() =>
      logger.info("Gui is delaying the AI new turn actions by 5 seconds ...")
      listener match {
        case Some(l) => l.newTurn()
        case _ =>
      }
      new Thread() {
        override def run() {
          Thread.sleep(5000)
          admiral ! NewTurn()
        }
      }.start()

    case ValidateAction(action, actor) =>
      val wa = new WrappedAction(action, actor)
      actionQue.append( wa )
      listener match {
        case Some(l) => l.actionReceived( wa )
        case _ =>
      }

    case Self() =>
      sender ! this

    case any =>
      logger.warn(s"GuiSupervisor received unknown message: $any")
  }

  def acceptAction(wa: WrappedAction) {
    val i = actionQue.indexOf( wa )
    if (i != -1) actionQue.remove( i )
    wa.actor ! ExecuteAction( wa.action )
  }

  def rejectAction(wa: WrappedAction) {
    val i = actionQue.indexOf( wa )
    if (i != -1) actionQue.remove( i )
    wa.actor ! RevokeAction( wa.action )
  }

}

class WrappedAction(val action: Action, val actor: ActorRef) {
  implicit val timeout: Timeout = 5 seconds;
  lazy val actorName = Await.result(actor ? Name(), timeout.duration).asInstanceOf[String]
  override def toString: String = s"$actorName: $action"
}

trait GuiSupervisorInterface {

  def actionReceived( wrappedAction: WrappedAction )
  def newTurn()

}