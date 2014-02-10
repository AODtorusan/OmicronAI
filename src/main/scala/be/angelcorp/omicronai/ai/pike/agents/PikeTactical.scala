package be.angelcorp.omicronai.ai.pike.agents

import collection.mutable
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import akka.actor.{ActorRef, Props}
import be.angelcorp.omicronai.{HexArea, Namer}
import be.angelcorp.omicronai.ai.{ActionExecutor, AI}
import be.angelcorp.omicronai.ai.pike.agents.squad.{NewSurveyRoi, SurveySquad, Squad}

class PikeTactical(val ai: AI, val aiExec: ActionExecutor) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val namer = new Namer[Class[_ <: Squad]](_.getSimpleName)

  val readyUnits = mutable.Set[ActorRef]()

  def act = {
    case AddMember(unit) =>
      logger.debug( s"$name was asked to assign unit $unit to a new Squad" )
      val newName = namer.nameFor(classOf[SurveySquad])
      val squad = context.actorOf(Props(classOf[SurveySquad], ai, aiExec ), name = newName)
      squad ! AddMember( unit )
      squad ! NewSurveyRoi( new HexArea(unit.checkLocation().get(), 20) )

    case NewTurn( turn ) =>
      logger.debug( s"$name is starting new turn actions" )
      readyUnits.clear()
      context.children.foreach( child => child ! NewTurn( turn ) )

    case Ready() =>
      readyUnits.add( sender )
      logger.debug( s"$name is marking $sender as ready. Waiting for: ${context.children.filterNot(readyUnits.contains)}" )
      if ( context.children.forall( readyUnits.contains ) )
        context.parent ! Ready()

    case ListMetadata() =>
      sender ! Nil

    case any =>
      logger.warn( s"Received an unknown tactical message: $any" )
  }

}
