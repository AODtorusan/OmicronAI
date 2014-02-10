package be.angelcorp.omicronai.algorithms

import scala.languageFeature.implicitConversions
import akka.actor.ActorRef
import akka.pattern.ask
import com.lyndir.omicron.api.model.{LevelType, Tile}
import com.lyndir.omicron.api.util.Maybe.Presence
import be.angelcorp.omicronai.{Direction, Location}
import be.angelcorp.omicronai.configuration.Configuration.config
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.world._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import be.angelcorp.omicronai.world.KnownState
import be.angelcorp.omicronai.world.LocationState
import scala.Some
import akka.util.Timeout

class MovementPathfinder( destination: Location, asset: Asset, world: ActorRef ) extends AStar {

  def heuristic(fromTile: Location) = fromTile δ destination

  val costGraph = new  WorldGraph[Null, Double] {
    implicit val game = asset.owner.getController.getGameController.getGame
    implicit val timeout: Timeout = Duration(1, TimeUnit.SECONDS)
    val inaccessible = Double.MaxValue

    override def tileAt(l: Location): Null = ???

    override def edgeAt(l: Location, d: Direction) = (l neighbour d).flatMap(target => {
      asset.mobility match {
        case Some(m) =>
          val isBlocked = Await.result(ask(world, LocationState(target)).mapTo[WorldState], timeout.duration) match {
            case KnownState(_, Some(obj), _) => true
            case GhostState(_, Some(obj), _) => true
            case _ => false
          }
          val level = Location.int2levelType(l.h)
          if (isBlocked) {
            None
          } else if ( d.dh == 0 ) {
            Some( m.costForMovingInLevel( level )   + config.pathfinder.layerPenalty( l.h ) )
          } else {
            Some( m.costForLevelingToLevel( level ) + config.pathfinder.layerChangePenalty + config.pathfinder.layerPenalty( target.h ) )
          }
        case _ => None
      }
    })
  }

  def goalReached(solution: AStarSolution) = destination == solution.tile

}
