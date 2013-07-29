package be.angelcorp.omicronai.agents

import collection.JavaConverters._
import com.lyndir.omicron.api.model.Player
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import akka.actor.{Actor, Props}
import be.angelcorp.omicronai.goals.{SquareArea, SurveyGoal}
import be.angelcorp.omicronai.{Location, StrategicMap}
import Location.level2int

class PikeTactical(aiPlayer: Player) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val name = "Tactical"

  lazy val tacticalMap = new StrategicMap(
    aiPlayer.getController.getGameController.listLevels().iterator().next().getSize
  )

  def act = {
    case AddMember(unit) =>
      logger.debug( s"$name was asked to assign unit ${unit} to a new Squad" )
      val squad = context.actorOf(Props(new Squad(aiPlayer)))
      squad ! AddMember( unit )

      val size = unit.getLocation.getLevel.getSize
      val level: Int = unit.getLocation.getLevel
      squad ! SetGoal( new SurveyGoal( new SquareArea(
        new Location(10, 10, level, size),
        new Location(20, 20, level, size)
      ) ) )

    case m: SubmitActions =>
      logger.debug( s"$name received request to submit all unit actions for validation, delegating to the Squads" )
      context.children.foreach( child => child.forward( m ) )

    case m: ValidateAction =>
      logger.trace( s"$name received order validation request, forwarding to the admiral" )
      context.parent.forward( m )

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
