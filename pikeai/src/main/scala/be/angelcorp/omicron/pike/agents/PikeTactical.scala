package be.angelcorp.omicron.pike.agents

import scala.collection.mutable
import akka.actor.{ActorRef, Props}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.PlayerKey
import be.angelcorp.omicron.base.{HexArea, Namer}
import be.angelcorp.omicron.base.ai.AI
import be.angelcorp.omicron.base.ai.actions.ActionExecutor
import be.angelcorp.omicron.base.bridge.NewTurn
import be.angelcorp.omicron.pike.agents.squad.{NewSurveyRoi, SurveySquad, Squad}

class PikeTactical(val ai: AI, protected val key: PlayerKey, val aiExec: ActionExecutor) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val namer = new Namer[Class[_ <: Squad]](_.getSimpleName)

  val readyUnits = mutable.Set[ActorRef]()

  override def preStart() {
    context.system.eventStream.subscribe(self, classOf[NewTurn])
  }

  def act = {
    case AddMember(unit) =>
      logger.debug( s"$name was asked to assign unit $unit to a new Squad" )
      val newName = namer.nameFor(classOf[SurveySquad])
      val squad = context.actorOf(Props(classOf[SurveySquad], ai, key, aiExec ), name = newName)
      squad ! AddMember( unit )
      //TODO: determine roi differently
      squad ! NewSurveyRoi( withSecurity { new HexArea(unit.checkLocation().get(), 20) } )

    case NewTurn( turn ) =>
      logger.debug( s"$name is starting new turn actions" )
      readyUnits.clear()

    case Ready() =>
      readyUnits.add( sender )
      logger.debug( s"$name is marking $sender as ready. Waiting for: ${context.children.filterNot(readyUnits.contains)}" )
      if ( context.children.forall( readyUnits.contains ) )
        context.parent ! Ready()

    case NotReady() =>
      readyUnits.remove( sender )
      logger.debug( s"$name is marking $sender as NOT ready. Waiting for: ${context.children.filterNot(readyUnits.contains)}" )

    case ListMetadata() =>
      sender ! Nil

    case any =>
      logger.warn( s"Received an unknown tactical message: $any" )
  }

}
