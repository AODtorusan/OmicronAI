package be.angelcorp.omicronai.agents

import org.slf4j.LoggerFactory
import collection.JavaConversions._
import akka.actor.{Props, ActorRef}
import com.lyndir.omnicron.api.model.Player
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicronai.assets.Asset

class Admiral(aiPlayer: Player) extends Agent {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  // Joint Chiefs of Staff
  var resourceGeneral: ActorRef = null
  var strategyGeneral: ActorRef = null
  var tacticalGeneral: ActorRef = null

  override def preStart {
    //resourceGeneral = context.actorOf(Props[DeafAgent], "resource general")
    //strategyGeneral = context.actorOf(Props[DeafAgent], "strategy general")
    tacticalGeneral = context.actorOf(Props(new PikeTactical(aiPlayer)), name = "TacticalGeneral")

    aiPlayer.getController.iterateObservableObjects(aiPlayer).iterator.foreach( unit => {
      tacticalGeneral ! NewUnit( unit )
    } )
  }

  def receive = {
    case NewTurn() =>
      tacticalGeneral ! ExecuteOrders()
    case event => logger.warn(s"Dude what the hell are you trying to tell me, I don't get this: $event")
  }

}

sealed abstract class AdmiralMessage
case class NewTurn() extends AdmiralMessage
