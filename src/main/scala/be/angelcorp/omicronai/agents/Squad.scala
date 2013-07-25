package be.angelcorp.omicronai.agents

import org.slf4j.LoggerFactory

import akka.pattern.ask
import be.angelcorp.omicronai.assets.Asset
import com.lyndir.omicron.api.model.Player
import scala.concurrent._
import scala.concurrent.duration._
import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicronai.goals.{NoGoal, Goal}
import be.angelcorp.omicronai.actions.Action
import akka.util.Timeout
import java.util.concurrent.atomic.AtomicInteger

class Squad(val aiPlayer: Player) extends Actor {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val name = s"Squad ${Squad.squadCount.incrementAndGet()}"
  var goal: Goal = new NoGoal

  logger.debug(s"Created a new squad: $name")

  def receive = {
    case AddMember( unit ) =>
      logger.debug(s"${name} was asked to absorb a new member: ${unit}")
      context.actorOf(Props(new Soldier(aiPlayer, new Asset(aiPlayer, unit) )) )

    case SetGoal( g ) =>
      logger.info(s"Reorienting ${name} to a new goal: $g")
      goal = g

    case ListMembers() =>
      logger.debug(s"${name} was asked for a list of members by $sender")
      sender ! context.children

    case Name() =>
      logger.trace(s"${name} was asked for a its name by $sender")
      sender ! name

    case SubmitActions()   => {
      logger.debug(s"${name} was asked to submit all actions for this turn for validation by its parent")
      implicit val timeout: Timeout = 5 seconds
      val members = context.children.par.map( ref =>
      try {
          val asset = Await.result(ref ? GetAsset(), timeout.duration).asInstanceOf[Asset]
          (ref, Some(asset))
        } catch {
          case e: Throwable =>
            logger.warn(s"Did not receive asset for a the soldier $ref", e)
            (ref, None)
        } )
      val actions = goal.findActions( members.toMap.seq )
      actions.foreach( entry => {
        val soldier = entry._1
        logger.debug(s"Requesting clearance for orders of ${Await.result(soldier ? Name(), timeout.duration).asInstanceOf[String]}")
        entry._2.foreach( action => context.parent ! ValidateAction(action, soldier) )
      } )
    }

    case any =>
      logger.debug(s"Invalid message received: $any")
  }

}

object Squad {

  var squadCount = new AtomicInteger(0)

}
