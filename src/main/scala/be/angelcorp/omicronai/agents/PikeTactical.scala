package be.angelcorp.omicronai.agents

import collection.mutable
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import akka.actor.{ActorRef, Props}
import com.lyndir.omicron.api.model.Player
import be.angelcorp.omicronai.{HexArea, Namer}
import be.angelcorp.omicronai.agents.squad.{NewSurveyRoi, Squad, SurveySquad}
import be.angelcorp.omicronai.bridge.NewTurn

class PikeTactical(aiPlayer: Player) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val name  = "Tactical"
  val namer = new Namer[Class[_ <: Squad]](_.getSimpleName)

  val readyUnits = mutable.Set[ActorRef]()

  lazy val cartographer =
    context.actorOf(Props(classOf[Cartographer], aiPlayer.getController.getGameController ), name = "Cartographer")

  def act = {
    case AddMember(unit) =>
      logger.debug( s"$name was asked to assign unit $unit to a new Squad" )
      val newName = namer.nameFor(classOf[SurveySquad])
      val squad = context.actorOf(Props(classOf[SurveySquad], aiPlayer, newName, cartographer ), name = newName)
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
