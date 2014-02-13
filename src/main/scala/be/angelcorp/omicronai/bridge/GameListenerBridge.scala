package be.angelcorp.omicronai.bridge

import akka.actor.Actor
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api._
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.IConstructorModule.IConstructionSite
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.ai.pike.agents.Self
import be.angelcorp.omicronai.ai.AI

class GameListenerBridge( key: (AI, PlayerKey), gameController: GameController ) extends GameListener with Actor {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  override def preStart() {
    key._1.withSecurity(key._2) {
      gameController.addGameListener( this )
    }
  }

  def receive = {
    case Self() => sender ! this
  }

  override def unhandled(message: Any) {
    logger.warn(s"Unhandled message for unit GameListenerBridge ($self): $message")
  }

  override def onPlayerReady(readyPlayer: IPlayer) {
    context.system.eventStream.publish( PlayerReady( readyPlayer ) )
  }

  override def onNewTurn(currentTurn: Turn) {
    context.system.eventStream.publish( NewTurn(currentTurn) )
  }

  override def onBaseDamaged(baseModule: IBaseModule, damage: ChangeInt) {
    context.system.eventStream.publish( BaseDamaged(baseModule, damage) )
  }

  override def onTileContents(tile: ITile, contents: Change[IGameObject]) {
    context.system.eventStream.publish( TileContentsChanged(tile, contents) )
  }

  override def onTileResources(tile: ITile, resourceType: ResourceType, resourceQuantity: ChangeInt) {
    context.system.eventStream.publish( TileResourcesChanged(tile, resourceType, resourceQuantity) )
  }

  override def onPlayerScore(player: IPlayer, score: ChangeInt) {
    context.system.eventStream.publish( PlayerScore(player, score) )
  }

  override def onPlayerGainedObject(player: IPlayer, gameObject: IGameObject) {
    context.system.eventStream.publish( PlayerGainedObject(player, gameObject) )
  }

  override def onPlayerLostObject(player: IPlayer, gameObject: IGameObject) {
    context.system.eventStream.publish( PlayerLostObject(player, gameObject) )
  }

  override def onUnitCaptured(gameObject: IGameObject, owner: Change[IPlayer]) {
    context.system.eventStream.publish( UnitCaptured(gameObject, owner) )
  }

  override def onUnitMoved(gameObject: IGameObject, location: Change[ITile]) {
    context.system.eventStream.publish( UnitMoved(gameObject, location) )
  }

  override def onUnitDied(gameObject: IGameObject) {
    context.system.eventStream.publish( UnitDied(gameObject) )
  }

  override def onContainerStockChanged(containerModule: IContainerModule, stock: ChangeInt) {
    context.system.eventStream.publish( ContainerStockChanged(containerModule, stock) )
  }

  override def onMobilityLeveled(mobilityModule: IMobilityModule, location: Change[ITile], remainingSpeed: ChangeDbl) {
    context.system.eventStream.publish( MobilityLeveled( mobilityModule, location, remainingSpeed ) )
  }

  override def onMobilityMoved(mobilityModule: IMobilityModule, location: Change[ITile], remainingSpeed: ChangeDbl) {
    context.system.eventStream.publish( MobilityMoved( mobilityModule, location, remainingSpeed ) )
  }

  override def onConstructorWorked(constructorModule: IConstructorModule, remainingSpeed: ChangeInt) {
    context.system.eventStream.publish( ConstructorWorked( constructorModule, remainingSpeed ) )
  }

  override def onConstructorTargeted(constructorModule: IConstructorModule, target: Change[IGameObject]) {
    context.system.eventStream.publish( ConstructorTargeted( constructorModule, target ) )
  }

  override def onConstructionSiteWorked(constructionSite: IConstructionSite, moduleType: PublicModuleType[_], remainingWork: ChangeInt) {
    context.system.eventStream.publish( ConstructionSiteWorked( constructionSite, moduleType.asInstanceOf[PublicModuleType[_ <: IModule]], remainingWork ) )
  }

  override def onWeaponFired(weaponModule: IWeaponModule, target: ITile, repeated: ChangeInt, ammunition: ChangeInt) {
    context.system.eventStream.publish( WeaponFired( weaponModule, target, repeated, ammunition ) )
  }

  override def onGameStarted(game: IGame) {
    context.system.eventStream.publish( GameStarted( game ) )
  }

  override def onGameEnded(game: IGame, victoryCondition: PublicVictoryConditionType, victor: IPlayer) {
    context.system.eventStream.publish( GameEnded( game, victoryCondition, victor ) )
  }

}

sealed abstract class GameListenerMessage
case class PlayerReady(readyPlayer: IPlayer) extends GameListenerMessage
case class NewTurn(currentTurn: Turn) extends GameListenerMessage
case class BaseDamaged(baseModule: IBaseModule, damage: ChangeInt) extends GameListenerMessage
case class TileContentsChanged(location: Location, contents: Change[IGameObject]) extends GameListenerMessage
case class TileResourcesChanged(location: Location, resourceType: ResourceType, resourceQuantity: ChangeInt) extends GameListenerMessage
case class PlayerScore(player: IPlayer, score: ChangeInt) extends GameListenerMessage
case class PlayerGainedObject(player: IPlayer, gameObject: IGameObject) extends GameListenerMessage
case class PlayerLostObject(player: IPlayer, gameObject: IGameObject) extends GameListenerMessage
case class UnitCaptured(gameObject: IGameObject, owner: Change[IPlayer]) extends GameListenerMessage
case class UnitMoved(gameObject: IGameObject, location: Change[ITile]) extends GameListenerMessage
case class UnitDied(gameObject: IGameObject) extends GameListenerMessage
case class ContainerStockChanged(containerModule: IContainerModule, stock: ChangeInt) extends GameListenerMessage
case class MobilityLeveled(mobilityModule: IMobilityModule, location: Change[ITile], remainingSpeed: ChangeDbl) extends GameListenerMessage
case class MobilityMoved(mobilityModule: IMobilityModule, location: Change[ITile], remainingSpeed: ChangeDbl) extends GameListenerMessage
case class ConstructorWorked(constructorModule: IConstructorModule, remainingSpeed: ChangeInt) extends GameListenerMessage
case class ConstructorTargeted(constructorModule: IConstructorModule, target: Change[IGameObject]) extends GameListenerMessage
case class ConstructionSiteWorked(constructionSite: IConstructionSite, moduleType: PublicModuleType[_ <: IModule], remainingWork: ChangeInt) extends GameListenerMessage
case class WeaponFired(weaponModule: IWeaponModule, target: Location, repeated: ChangeInt, ammunition: ChangeInt) extends GameListenerMessage
case class GameStarted(game: IGame) extends GameListenerMessage
case class GameEnded(game: IGame, victoryCondition: PublicVictoryConditionType, victor: IPlayer) extends GameListenerMessage

