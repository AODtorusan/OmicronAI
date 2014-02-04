package be.angelcorp.omicronai

import com.lyndir.omicron.api.{ChangeDbl, Change, ChangeInt, GameListener}
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.ConstructorModule.ConstructionSite
import com.lyndir.omicron.api.model.IConstructorModule.IConstructionSite

class GameListenerLogger extends GameListener {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  override def onPlayerReady(readyPlayer: IPlayer): Unit =
    logger.trace(s"Event: onPlayerReady( readyPlayer = $readyPlayer )")

  override def onNewTurn(currentTurn: Turn): Unit =
    logger.trace(s"Event: onNewTurn(currentTurn = $currentTurn )")

  override def onBaseDamaged(baseModule: IBaseModule, damage: ChangeInt): Unit =
    logger.trace(s"Event: onBaseDamaged(baseModule = $baseModule, damage = $damage )")

  override def onTileContents(tile: ITile, contents: Change[IGameObject]): Unit =
    logger.trace(s"Event: onTileContents(tile = $tile, contents = $contents )")

  override def onTileResources(tile: ITile, resourceType: ResourceType, resourceQuantity: ChangeInt): Unit =
    logger.trace(s"Event: onTileResources(tile = $tile, resourceType = $resourceType, resourceQuantity = $resourceQuantity )")

  override def onPlayerScore(player: IPlayer, score: ChangeInt): Unit =
    logger.trace(s"Event: onPlayerScore(player = $player, score = $score )")

  override def onPlayerGainedObject(player: IPlayer, gameObject: IGameObject): Unit =
    logger.trace(s"Event: onPlayerGainedObject(player = $player, gameObject = $gameObject )")

  override def onPlayerLostObject(player: IPlayer, gameObject: IGameObject): Unit =
    logger.trace(s"Event: onPlayerLostObject(player = $player, gameObject = $gameObject )")

  override def onUnitCaptured(gameObject: IGameObject, owner: Change[IPlayer]): Unit =
    logger.trace(s"Event: onUnitCaptured(gameObject = $gameObject, owner = $owner )")

  override def onUnitMoved(gameObject: IGameObject, location: Change[ITile]): Unit =
    logger.trace(s"Event: onUnitMoved(gameObject = $gameObject, location = $location )")

  override def onUnitDied(gameObject: IGameObject): Unit =
    logger.trace(s"Event: onUnitDied(gameObject = $gameObject)")

  override def onContainerStockChanged(containerModule: IContainerModule, stock: ChangeInt): Unit =
    logger.trace(s"Event: onContainerStockChanged(containerModule = $containerModule, stock = $stock )")

  override def onMobilityLeveled(mobilityModule: IMobilityModule, location: Change[ITile], remainingSpeed: ChangeDbl): Unit =
    logger.trace(s"Event: onMobilityLeveled(mobilityModule = $mobilityModule, location = $location, remainingSpeed = $remainingSpeed )")

  override def onMobilityMoved(mobilityModule: IMobilityModule, location: Change[ITile], remainingSpeed: ChangeDbl): Unit =
    logger.trace(s"Event: onMobilityMoved(mobilityModule = $mobilityModule, location = $location, remainingSpeed = $remainingSpeed )")

  override def onConstructorWorked(constructorModule: IConstructorModule, remainingSpeed: ChangeInt): Unit =
    logger.trace(s"Event: onConstructorWorked(constructorModule = $constructorModule, remainingSpeed = $remainingSpeed )")

  override def onConstructorTargeted(constructorModule: IConstructorModule, target: Change[IGameObject]): Unit =
    logger.trace(s"Event: onConstructorTargeted(constructorModule = $constructorModule, target = $target )")

  override def onConstructionSiteWorked(constructionSite: IConstructionSite, moduleType: PublicModuleType[_], remainingWork: ChangeInt): Unit =
    logger.trace(s"Event: onConstructionSiteWorked(constructionSite = $constructionSite, moduleType = $moduleType, remainingWork = $remainingWork )")

  override def onWeaponFired(weaponModule: IWeaponModule, target: ITile, repeated: ChangeInt, ammunition: ChangeInt): Unit =
    logger.trace(s"Event: onWeaponFired(weaponModule = $weaponModule, target = $target, repeated = $repeated, ammunition = $ammunition )")

  override def onGameStarted(game: IGame): Unit =
    logger.trace(s"Event: onGameStarted(game = $game )")

  override def onGameEnded(game: IGame, victoryCondition: PublicVictoryConditionType, victor: IPlayer): Unit =
    logger.trace(s"Event: onGameEnded(game = $game, victoryCondition = $victoryCondition, victor = $victor )")

}
