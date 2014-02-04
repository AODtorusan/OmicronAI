package be.angelcorp.omicronai.world

import akka.actor.{Props, ActorSystem, Actor}
import akka.dispatch.{UnboundedPriorityMailbox, PriorityGenerator}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.typesafe.config.Config
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.util.Maybe.Presence
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.algorithms.Field

class World(player: Player, sz: WorldSize) extends Actor {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  private implicit val game = player.getController.getGameController.getGame
  private val gameState = Field.fill[WorldState](sz)(LazyEmptyState)

  private def checkResources(t: Tile) =
    ResourceType.values().map( r => {
      val q = t.checkResourceQuantity( r )
      (r, q.presence() match {
        case Presence.PRESENT => q.get().intValue()
        case _ => 0
      } )
    } ).toMap

  // Current state as known bu the game
  private def realState(l: Location) = {
    val t = Location.location2tile(l)
    val content   = t.checkContents()
    content.presence() match {
      case Presence.PRESENT => KnownState(l, Some(content.get), checkResources(t))
      case Presence.ABSENT  => KnownState(l, None, checkResources(t))
      case Presence.UNKNOWN => UnknownState
    }
  }

  private def updateLocation(l: Location) {
    //logger.trace(s"Updating world state on $l")
    realState(l) match {
      // If we can see the tile, update the map state
      case newState: KnownState =>
        gameState(l) = newState
      // If we could see the tile, but not anymore, make it a ghost tile
      case UnknownState => gameState(l) match {
        case current: KnownState => gameState(l) = current.toGhost
        case LazyEmptyState      => gameState(l) = UnknownState
        case _ => // Do nothing, already ghost or unknown
      }
      case _ => throw new UnsupportedOperationException // Ghost state is never returned
    }
  }

  def getState(l: Location): WorldState =
    gameState(l) match {
      case LazyEmptyState =>
        updateLocation(l)
        gameState(l)
      case state =>
        state
    }

  def receive: Actor.Receive = {
    case ReloadLocation(l)   => updateLocation(l)
    case ReloadReady()       => sender ! true // True due to priority handling of messages
    case LocationState(l)    => sender ! getState(l)
    case LocationStates(l)   => sender ! l.map( loc => getState(loc) )
    case m: GetWorldListener => context.children.head.forward(m)
  }

}

object World {

  def apply( player: Player, size: WorldSize ) =
    Props(classOf[World], player, size).withDispatcher("akka.world-dispatcher")

  def withInterface(player: Player, size: WorldSize) = {
    val system = ActorSystem("WorldActorSystem")
    val world = system.actorOf( World(player, size) )
    new WorldInterface(world)
  }

}

sealed abstract class WorldActorMsg
private case class GetWorldListener()               extends WorldActorMsg
private case class ReloadLocation(l: Location)      extends WorldActorMsg
private case class ReloadReady()                    extends WorldActorMsg
case class LocationState (l: Location)              extends WorldActorMsg
case class LocationStates(l: Seq[Location])         extends WorldActorMsg

class PrioritizedMailbox(settings: ActorSystem.Settings, cfg: Config) extends UnboundedPriorityMailbox(
  PriorityGenerator {
    case m: ReloadLocation  => 0
    case m: ReloadReady     => 3
    case m: LocationState   => 6
    case m: LocationStates  => 6
    case _                  => 6
  } )

sealed abstract class WorldState
object LazyEmptyState extends WorldState
object UnknownState   extends WorldState
case class GhostState(location: Location, content: Option[IGameObject], resources: Map[ResourceType, Int]) extends WorldState
case class KnownState(location: Location, content: Option[IGameObject], resources: Map[ResourceType, Int]) extends WorldState {
  def toGhost = GhostState(location, content, resources)
}


