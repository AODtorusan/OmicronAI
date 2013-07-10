package be.angelcorp.omicronai.agents

import org.slf4j.LoggerFactory

import akka.pattern.ask
import be.angelcorp.omicronai.assets.Asset
import com.lyndir.omnicron.api.model.{Player, GameObject}
import scala.concurrent._
import akka.actor.Props
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicronai.goals.{NoGoal, Goal}
import be.angelcorp.omicronai.actions.Action
import scala.concurrent.duration._
import akka.util.Timeout

class Squad(val aiPlayer: Player) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  var goal: Goal = new NoGoal

  def receive = {
    case AddMember( unit ) => context.actorOf(Props(new Soldier(aiPlayer, new Asset(aiPlayer, unit) )) )
    case SetGoal( g )      => goal = g
    case ListMembers()     => sender ! context.children
    case ExecuteOrders()   => {
      val members = context.children.par.map( ref =>
        try {
          implicit val timeout: Timeout = 5 seconds
          val asset = Await.result(ref ? GetAsset(), 5 seconds).asInstanceOf[Asset]
          (ref, Some(asset))
        } catch {
          case e: Throwable =>
            logger.warn(s"Did not receive asset for a the soldier $ref", e)
            (ref, None)
        } )
      val actions = goal.findActions( members.toMap.seq )

      actions.foreach( entry => entry._1 ! ExecuteActions( entry._2 ) )
    }
    case any => logger.debug(s"Invalid message received: $any")
  }

}

sealed abstract class SquadMessage
case class AddMember(  unit: GameObject ) extends SquadMessage
case class SetGoal(    goal: Goal       ) extends SquadMessage
case class ExecuteActions( actions: Seq[Action] ) extends SquadMessage
case class ListMembers() extends SquadMessage
