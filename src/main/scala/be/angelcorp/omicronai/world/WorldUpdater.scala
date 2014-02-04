package be.angelcorp.omicronai.world

import akka.actor.ActorRef
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.{ChangeInt, Change, GameListener}
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.util.Maybe.Presence
import be.angelcorp.omicronai.Location

class WorldUpdater( world: ActorRef ) extends GameListener {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  private def viewRange(gameObject: IGameObject) =
    gameObject.getModule(PublicModuleType.BASE, 0).get().getViewRange

  override def onTileContents(tile: ITile, contents: Change[IGameObject]): Unit =
    world ! ReloadLocation(tile)

  override def onTileResources(tile: ITile, resourceType: ResourceType, resourceQuantity: ChangeInt): Unit =
    world ! ReloadLocation(tile)

  override def onUnitMoved(gameObject: IGameObject, location: Change[ITile]) = {
    val vr = viewRange(gameObject)

    val couldSee = (location.getFrom: Location).range( vr )
    val canSee   = (location.getTo:   Location).range( vr )
    val visibleToInvisible = couldSee.diff(canSee)
    val invisibleToVisible = canSee.diff(couldSee)
    visibleToInvisible.foreach( l => world ! ReloadLocation(l) )
    invisibleToVisible.foreach( l => world ! ReloadLocation(l) )
  }

  override def onPlayerGainedObject(player: IPlayer, gameObject: IGameObject): Unit = {
    val tile = gameObject.checkLocation()
    tile.presence() match {
      case Presence.PRESENT => (tile.get: Location).range( viewRange(gameObject) ).foreach( l => world ! ReloadLocation(l) )
      case _ =>
    }
  }

  override def onPlayerLostObject(player: IPlayer, gameObject: IGameObject): Unit = {
    val tile = gameObject.checkLocation()
    tile.presence() match {
      case Presence.PRESENT => (tile.get: Location).range( viewRange(gameObject) ).foreach( l => world ! ReloadLocation(l) )
      case _ =>
    }
  }

}
