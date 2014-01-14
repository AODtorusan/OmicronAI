package be.angelcorp.omicronai.ai.pike.agents

import com.lyndir.omicron.api._
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.ConstructorModule.ConstructionSite
import be.angelcorp.omicronai.Location

class GameListenerBridge extends GameListener with Agent {

  def name = "GameListenerBridge"
  def act = Map.empty

  override def onPlayerReady(readyPlayer: Player) {
    context.parent ! PlayerReady( readyPlayer )
  }

  override def onNewTurn(currentTurn: Turn) {
    context.parent ! NewTurn(currentTurn)
    context.parent ! Ready()
  }

  override def onBaseDamaged(baseModule: BaseModule, damage: ChangeInt) {
    context.parent ! BaseDamaged(baseModule, damage)
  }

  override def onTileContents(tile: Tile, contents: Change[GameObject]) {
    context.parent ! TileContentsChanged(tile, contents)
  }

  override def onTileResources(tile: Tile, resourceType: ResourceType, resourceQuantity: ChangeInt) {
    context.parent ! TileResourcesChanged(tile, resourceType, resourceQuantity)
  }

  override def onPlayerScore(player: Player, score: ChangeInt) {
    context.parent ! PlayerScore(player, score)
  }

  override def onPlayerGainedObject(player: Player, gameObject: GameObject) {
    context.parent ! PlayerGainedObject(player, gameObject)
  }

  override def onPlayerLostObject(player: Player, gameObject: GameObject) {
    context.parent ! PlayerLostObject(player, gameObject)
  }

  override def onUnitCaptured(gameObject: GameObject, owner: Change[Player]) {
    context.parent ! UnitCaptured(gameObject, owner)
  }

  override def onUnitMoved(gameObject: GameObject, location: Change[Tile]) {
    context.parent ! UnitMoved(gameObject, location)
  }

  override def onUnitDied(gameObject: GameObject) {
    context.parent ! UnitDied(gameObject)
  }

  override def onContainerStockChanged(containerModule: ContainerModule, stock: ChangeInt) {
    context.parent ! ContainerStockChanged(containerModule, stock)
  }

  override def onMobilityLeveled(mobilityModule: MobilityModule, location: Change[Tile], remainingSpeed: ChangeDbl) {
    context.parent ! MobilityLeveled( mobilityModule, location, remainingSpeed )
  }

  override def onMobilityMoved(mobilityModule: MobilityModule, location: Change[Tile], remainingSpeed: ChangeDbl) {
    context.parent ! MobilityMoved( mobilityModule, location, remainingSpeed )
  }

  override def onConstructorWorked(constructorModule: ConstructorModule, remainingSpeed: ChangeInt) {
    context.parent ! ConstructorWorked( constructorModule, remainingSpeed )
  }

  override def onConstructorTargeted(constructorModule: ConstructorModule, target: Change[GameObject]) {
    context.parent ! ConstructorTargeted( constructorModule, target )
  }


  override def onConstructionSiteWorked(constructionSite: ConstructionSite, moduleType: ModuleType[_<:Module], remainingWork: ChangeInt) {
    context.parent ! ConstructionSiteWorked( constructionSite, moduleType, remainingWork )
  }

  override def onWeaponFired(weaponModule: WeaponModule, target: Tile, repeated: ChangeInt, ammunition: ChangeInt) {
    context.parent ! WeaponFired( weaponModule, target, repeated, ammunition )
  }

  override def onGameStarted(game: Game) {
    context.parent ! GameStarted( game )
  }

  override def onGameEnded(game: Game, victoryCondition: VictoryConditionType, victor: Player) {
    context.parent ! GameEnded( game, victoryCondition, victor )
  }

}

sealed abstract class GameListenerMessage
case class PlayerReady(readyPlayer: Player) extends GameListenerMessage
case class NewTurn(currentTurn: Turn) extends GameListenerMessage
case class BaseDamaged(baseModule: BaseModule, damage: ChangeInt) extends GameListenerMessage
case class TileContentsChanged(location: Location, contents: Change[GameObject]) extends GameListenerMessage
case class TileResourcesChanged(location: Location, resourceType: ResourceType, resourceQuantity: ChangeInt) extends GameListenerMessage
case class PlayerScore(player: Player, score: ChangeInt) extends GameListenerMessage
case class PlayerGainedObject(player: Player, gameObject: GameObject) extends GameListenerMessage
case class PlayerLostObject(player: Player, gameObject: GameObject) extends GameListenerMessage
case class UnitCaptured(gameObject: GameObject, owner: Change[Player]) extends GameListenerMessage
case class UnitMoved(gameObject: GameObject, location: Change[Tile]) extends GameListenerMessage
case class UnitDied(gameObject: GameObject) extends GameListenerMessage
case class ContainerStockChanged(containerModule: ContainerModule, stock: ChangeInt) extends GameListenerMessage
case class MobilityLeveled(mobilityModule: MobilityModule, location: Change[Tile], remainingSpeed: ChangeDbl) extends GameListenerMessage
case class MobilityMoved(mobilityModule: MobilityModule, location: Change[Tile], remainingSpeed: ChangeDbl) extends GameListenerMessage
case class ConstructorWorked(constructorModule: ConstructorModule, remainingSpeed: ChangeInt) extends GameListenerMessage
case class ConstructorTargeted(constructorModule: ConstructorModule, target: Change[GameObject]) extends GameListenerMessage
case class ConstructionSiteWorked(constructionSite: ConstructorModule.ConstructionSite, moduleType: ModuleType[_ <: Module], remainingWork: ChangeInt) extends GameListenerMessage
case class WeaponFired(weaponModule: WeaponModule, target: Location, repeated: ChangeInt, ammunition: ChangeInt) extends GameListenerMessage
case class GameStarted(game: Game) extends GameListenerMessage
case class GameEnded(game: Game, victoryCondition: VictoryConditionType, victor: Player) extends GameListenerMessage

