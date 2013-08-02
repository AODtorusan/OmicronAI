package be.angelcorp.omicronai.agents

import collection.mutable
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import akka.actor.{ActorRef, Props}
import com.lyndir.omicron.api.model.Player
import be.angelcorp.omicronai.{SquareArea, Namer, Location}
import be.angelcorp.omicronai.Location._
import be.angelcorp.omicronai.agents.squad.{NewSurveyRoi, Squad, SurveySquad}

class PikeTactical(aiPlayer: Player) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val name  = "Tactical"
  val namer = new Namer[Class[_ <: Squad]](_.getSimpleName)

  val readyUnits = mutable.Set[ActorRef]()

  lazy val cartographer = context.actorOf(Props(new Cartographer( aiPlayer.getController.getGameController )), name = "Cartographer")

  def act = {
    case AddMember(unit) =>
      logger.debug( s"$name was asked to assign unit $unit to a new Squad" )
      val newName = namer.nameFor(classOf[SurveySquad])
      val squad = context.actorOf(Props(new SurveySquad(aiPlayer, newName, cartographer )), name = newName)
      squad ! AddMember( unit )

      val size = unit.getLocation.getLevel.getSize
      val level: Int = unit.getLocation.getLevel
      squad ! NewSurveyRoi( new SquareArea(
        new Location(10, 10, level, size),
        new Location(20, 20, level, size)
      ) )

    case NewTurn() =>
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

    case Name() =>
      logger.trace(s"$name was asked for a its name by $sender")
      sender ! name

    case ListMembers() =>
      logger.debug(s"$name was asked for a list of members by $sender")
      sender ! context.children

    case any =>
      logger.warn( s"Received an unknown tactical message: $any" )
  }

}
