package be.angelcorp.omicronai.ai.pike.agents

import collection.mutable
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import akka.actor.{ActorRef, Props}
import com.lyndir.omicron.api.model.Player
import be.angelcorp.omicronai.{HexArea, Namer}
import be.angelcorp.omicronai.ai.pike.agents.squad.{NewSurveyRoi, SurveySquad, Squad}
import be.angelcorp.omicronai.world.World

class PikeTactical(aiPlayer: Player, world: ActorRef) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val namer = new Namer[Class[_ <: Squad]](_.getSimpleName)

  val readyUnits = mutable.Set[ActorRef]()

  def act = {
    case AddMember(unit) =>
      logger.debug( s"$name was asked to assign unit $unit to a new Squad" )
      val newName = namer.nameFor(classOf[SurveySquad])
      val squad = context.actorOf(Props(classOf[SurveySquad], aiPlayer, world ), name = newName)
      squad ! AddMember( unit )
      squad ! NewSurveyRoi( new HexArea(unit.location, 20) )

    case NewTurn( currentTurn ) =>
      logger.debug( s"$name is starting new turn actions" )
      readyUnits.clear()
      context.children.foreach( child => child ! SubmitActions() )

    case Ready() =>
      readyUnits.add( sender )
      logger.debug( s"$name is marking $sender as ready. Waiting for: ${context.children.filterNot(readyUnits.contains)}" )
      if ( context.children.forall( readyUnits.contains ) )
        context.parent ! Ready()

    case ValidateAction(action, unit) =>
      logger.trace( s"$name received order validation request" )
      unit ! ExecuteAction( action )

    case ListMetadata() =>
      sender ! Nil

    case any =>
      logger.warn( s"Received an unknown tactical message: $any" )
  }

}
