package be.angelcorp.omicronai.agents

import collection.JavaConverters._
import com.lyndir.omnicron.api.model.{GameObject, Player}
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import akka.actor.Props
import be.angelcorp.omicronai.goals.{SquareArea, SurveyGoal}
import be.angelcorp.omicronai.{Location, StrategicMap}
import Location.level2int

class PikeTactical(aiPlayer: Player) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val tacticalMap = new StrategicMap( aiPlayer.getObjects.asScala.headOption match {
    case Some( unit ) => unit.getLocation.getLevel.getLevelSize
    case None => throw new RuntimeException( "Cannot create tactical map, cannot derive map size from the location of the first unit. No such unit exists!" )
  } )

  def receive = {
    case NewUnit(unit) =>
      val squad = context.actorOf(Props(new Squad(aiPlayer)))
      squad ! AddMember( unit )

      val size = tacticalMap.size
      val level: Int = unit.getLocation.getLevel
      squad ! SetGoal( new SurveyGoal( new SquareArea(
        new Location(0, 0, level, size),
        new Location(size.getWidth - 1, size.getHeight - 1, level, size)
      ) ) )
    case ExecuteOrders() => context.children.foreach( _ ! ExecuteOrders() )
    case any => logger.warn( s"Received an unknown tactical message: $any" )
  }

}

sealed abstract class TacticalMessage
case class NewUnit( unit: GameObject )
case class ExecuteOrders()
