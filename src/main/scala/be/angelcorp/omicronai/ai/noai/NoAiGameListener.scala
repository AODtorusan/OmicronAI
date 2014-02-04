package be.angelcorp.omicronai.ai.noai

import com.lyndir.omicron.api._
import be.angelcorp.omicronai.ai.noai.gui.NoAiGui
import com.lyndir.omicron.api.model._
import be.angelcorp.omicronai._
import com.lyndir.omicron.api.model.IConstructorModule.IConstructionSite
import be.angelcorp.omicronai.Conversions._
import be.angelcorp.omicronai.Present

class NoAiGameListener( gui: NoAiGui ) extends GameListener {

  override def onPlayerReady(readyPlayer: IPlayer) {
    gui.message( s"Player ${readyPlayer.getName} (${readyPlayer.getPlayerID}) is ready." )
  }

  override def onNewTurn(currentTurn: Turn) {
    gui.message( s"Starting turn ${currentTurn.getNumber}." )
  }

  override def onBaseDamaged(baseModule: IBaseModule, damage: ChangeInt) {
    gui.message( s"Base building (${baseModule}) damaged (-$damage)." )
  }

  override def onTileContents(tile: ITile, contents: Change[IGameObject]) {
    gui.message( s"Contents of ${tile: Location} changed to ${contents}." )
  }

  override def onTileResources(tile: ITile, resourceType: ResourceType, resourceQuantity: ChangeInt) {
    gui.message( s"Resources of ${tile: Location} changed to $resourceQuantity of $resourceType." )
  }

  override def onPlayerScore(player: IPlayer, score: ChangeInt) {
    gui.message( s"Score of player ${player.getName} (${player.getPlayerID}) changed to ${score.getTo}." )
  }

  override def onPlayerGainedObject(player: IPlayer, gameObject: IGameObject) {
    gui.message( s"New object for player ${player.getName} (${player.getPlayerID}): ${gameObject.getType.getTypeName}." )
  }

  override def onPlayerLostObject(player: IPlayer, gameObject: IGameObject) {
    gui.message( s"Player ${player.getName} (${player.getPlayerID}) lost object: ${gameObject.getType.getTypeName}." )
  }

  override def onUnitCaptured(gameObject: IGameObject, owner: Change[IPlayer]) {
    gui.message( s"${gameObject.getType.getTypeName} of ${owner.getFrom.getName} (${owner.getFrom.getPlayerID}) captured by ${owner.getTo.getName} (${owner.getTo.getPlayerID})." )
  }

  override def onUnitMoved(gameObject: IGameObject, location: Change[ITile]) {
    gui.message( s"${gameObject.getType.getTypeName} moved from ${location.getFrom: Location} to ${location.getTo: Location}." )
  }

  override def onUnitDied(gameObject: IGameObject) {
    gui.message( s"Unit died: ${gameObject.getType.getTypeName}." )
  }

  override def onContainerStockChanged(containerModule: IContainerModule, stock: ChangeInt) {
    gui.message( s"${containerModule.getGameObject.getType.getTypeName} is now storing ${stock.getTo} units of ${containerModule.getResourceType}." )
  }

  override def onMobilityLeveled(mobilityModule: IMobilityModule, location: Change[ITile], remainingSpeed: ChangeDbl) {
    gui.message( s"${mobilityModule.getGameObject.getType.getTypeName} level to ${location.getTo: Location}." )
  }

  override def onMobilityMoved(mobilityModule: IMobilityModule, location: Change[ITile], remainingSpeed: ChangeDbl) {
    gui.message( s"${mobilityModule.getGameObject.getType.getTypeName} moved to ${location.getTo: Location}." )
  }

  override def onConstructorWorked(constructorModule: IConstructorModule, remainingSpeed: ChangeInt) {
    gui.message( s"${constructorModule.getGameObject.getType.getTypeName} is building. ${remainingSpeed.getTo} build units remaining." )
  }

  override def onConstructorTargeted(constructorModule: IConstructorModule, target: Change[IGameObject]) {
    gui.message( s"${constructorModule.getGameObject.getType.getTypeName} is now building on ${target.getTo.getType.getTypeName}." )
  }

  override def onConstructionSiteWorked(constructionSite: IConstructionSite, moduleType: PublicModuleType[_], remainingWork: ChangeInt): Unit = {
    gui.message( s"Construction site at ${(constructionSite.checkLocation: Maybe[_ <: ITile]) match {
      case Present( l ) => l: Location
      case Absent => "<missing>"
      case Unknown => "<unknown>"
    }} only requires ${remainingWork.getTo} build units." )
  }

  override def onWeaponFired(weaponModule: IWeaponModule, target: ITile, repeated: ChangeInt, ammunition: ChangeInt) {
    gui.message( s"Weapon ${weaponModule} fired site at ${target: Location}." )
  }

  override def onGameStarted(game: IGame) {
    gui.message( s"Game started." )
  }

  override def onGameEnded(game: IGame, victoryCondition: PublicVictoryConditionType, victor: IPlayer) {
    gui.message( s"Game ended, ${victor.getName} (${victor.getPlayerID}) won." )
  }

}
