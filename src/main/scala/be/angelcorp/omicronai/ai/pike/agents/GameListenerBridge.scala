package be.angelcorp.omicronai.ai.pike.agents

import com.lyndir.omicron.api._
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.IConstructorModule.IConstructionSite
import be.angelcorp.omicronai.Location

class GameListenerBridge extends GameListener with Agent {

  def name = "GameListenerBridge"
  def act = Map.empty

  override def onPlayerReady(readyPlayer: IPlayer) {
    context.parent ! PlayerReady( readyPlayer )
  }

  override def onNewTurn(currentTurn: Turn) {
    context.parent ! NewTurn(currentTurn)
    context.parent ! Ready()
  }

  override def onBaseDamaged(baseModule: IBaseModule, damage: ChangeInt) {
    context.parent ! BaseDamaged(baseModule, damage)
  }

  override def onTileContents(tile: ITile, contents: Change[IGameObject]) {
    context.parent ! TileContentsChanged(tile, contents)
  }

  override def onTileResources(tile: ITile, resourceType: ResourceType, resourceQuantity: ChangeInt) {
    context.parent ! TileResourcesChanged(tile, resourceType, resourceQuantity)
  }

  override def onPlayerScore(player: IPlayer, score: ChangeInt) {
    context.parent ! PlayerScore(player, score)
  }

  override def onPlayerGainedObject(player: IPlayer, gameObject: IGameObject) {
    context.parent ! PlayerGainedObject(player, gameObject)
  }

  override def onPlayerLostObject(player: IPlayer, gameObject: IGameObject) {
    context.parent ! PlayerLostObject(player, gameObject)
  }

  override def onUnitCaptured(gameObject: IGameObject, owner: Change[IPlayer]) {
    context.parent ! UnitCaptured(gameObject, owner)
  }

  override def onUnitMoved(gameObject: IGameObject, location: Change[ITile]) {
    context.parent ! UnitMoved(gameObject, location)
  }

  override def onUnitDied(gameObject: IGameObject) {
    context.parent ! UnitDied(gameObject)
  }

  override def onContainerStockChanged(containerModule: IContainerModule, stock: ChangeInt) {
    context.parent ! ContainerStockChanged(containerModule, stock)
  }

  override def onMobilityLeveled(mobilityModule: IMobilityModule, location: Change[ITile], remainingSpeed: ChangeDbl) {
    context.parent ! MobilityLeveled( mobilityModule, location, remainingSpeed )
  }

  override def onMobilityMoved(mobilityModule: IMobilityModule, location: Change[ITile], remainingSpeed: ChangeDbl) {
    context.parent ! MobilityMoved( mobilityModule, location, remainingSpeed )
  }

  override def onConstructorWorked(constructorModule: IConstructorModule, remainingSpeed: ChangeInt) {
    context.parent ! ConstructorWorked( constructorModule, remainingSpeed )
  }

  override def onConstructorTargeted(constructorModule: IConstructorModule, target: Change[IGameObject]) {
    context.parent ! ConstructorTargeted( constructorModule, target )
  }

  override def onConstructionSiteWorked(constructionSite: IConstructionSite, moduleType: PublicModuleType[_], remainingWork: ChangeInt) {
    context.parent ! ConstructionSiteWorked( constructionSite, moduleType.asInstanceOf[PublicModuleType[_ <: IModule]], remainingWork )
  }

  override def onWeaponFired(weaponModule: IWeaponModule, target: ITile, repeated: ChangeInt, ammunition: ChangeInt) {
    context.parent ! WeaponFired( weaponModule, target, repeated, ammunition )
  }

  override def onGameStarted(game: IGame) {
    context.parent ! GameStarted( game )
  }

  override def onGameEnded(game: IGame, victoryCondition: PublicVictoryConditionType, victor: IPlayer) {
    context.parent ! GameEnded( game, victoryCondition, victor )
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

