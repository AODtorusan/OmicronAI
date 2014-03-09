package be.angelcorp.omicron.noai

import com.lyndir.omicron.api._
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.IConstructorModule.IConstructionSite
import be.angelcorp.omicron.base._
import be.angelcorp.omicron.base.Conversions._
import be.angelcorp.omicron.noai.gui.NoAiGui

class NoAiGameListener( gui: NoAiGui ) extends GameListener {

  lazy val messageBus = gui.guiMessageBus
  implicit def game = gui.noai.game

  override def onPlayerReady(readyPlayer: IPlayer) {
    messageBus.publish( new PlainMessage( s"Player ${readyPlayer.getName} (${readyPlayer.getPlayerID}) is ready." ) )
  }

  override def onNewTurn(currentTurn: Turn) {
    messageBus.publish( new PlainMessage( s"Starting turn ${currentTurn.getNumber}." ) )
  }

  override def onBaseDamaged(baseModule: IBaseModule, damage: ChangeInt) {
    messageBus.publish( new LocatedMessage( s"Base building ($baseModule) damaged (-$damage).", baseModule.getGameObject.checkLocation().get ) )
  }

  override def onTileContents(tile: ITile, contents: Change[IGameObject]) {
    messageBus.publish( new LocatedMessage( s"Contents of ${tile: Location} changed to ${contents}.", tile ) )
  }

  override def onTileResources(tile: ITile, resourceType: ResourceType, resourceQuantity: ChangeInt) {
    messageBus.publish( new LocatedMessage( s"Resources of ${tile: Location} changed to $resourceQuantity of $resourceType.", tile ) )
  }

  override def onPlayerScore(player: IPlayer, score: ChangeInt) {
    messageBus.publish( new PlainMessage( s"Score of player ${player.getName} (${player.getPlayerID}) changed to ${score.getTo}." ) )
  }

  override def onPlayerGainedObject(player: IPlayer, gameObject: IGameObject) {
    messageBus.publish( new LocatedMessage( s"New object for player ${player.getName} (${player.getPlayerID}): ${gameObject.getType.getTypeName}.", gameObject.checkLocation().get ) )
  }

  override def onPlayerLostObject(player: IPlayer, gameObject: IGameObject) {
    messageBus.publish( new PlainMessage( s"Player ${player.getName} (${player.getPlayerID}) lost object: ${gameObject.getType.getTypeName}." ) )
  }

  override def onUnitCaptured(gameObject: IGameObject, owner: Change[IPlayer]) {
    messageBus.publish( new LocatedMessage( s"${gameObject.getType.getTypeName} of ${owner.getFrom.getName} (${owner.getFrom.getPlayerID}) captured by ${owner.getTo.getName} (${owner.getTo.getPlayerID}).", gameObject.checkLocation().get() ) )
  }

  override def onUnitMoved(gameObject: IGameObject, location: Change[ITile]) {
    messageBus.publish( new LocatedMessage( s"${gameObject.getType.getTypeName} moved from ${location.getFrom: Location} to ${location.getTo: Location}.", location.getTo ) )
  }

  override def onUnitDied(gameObject: IGameObject) {
    messageBus.publish( new PlainMessage( s"Unit died: ${gameObject.getType.getTypeName}." ) )
  }

  override def onContainerStockChanged(containerModule: IContainerModule, stock: ChangeInt) {
    messageBus.publish( new LocatedMessage( s"${containerModule.getGameObject.getType.getTypeName} is now storing ${stock.getTo} units of ${containerModule.getResourceType}.", containerModule.getGameObject.checkLocation().get ) )
  }

  override def onMobilityLeveled(mobilityModule: IMobilityModule, location: Change[ITile], remainingSpeed: ChangeDbl) {
    messageBus.publish( new LocatedMessage( s"${mobilityModule.getGameObject.getType.getTypeName} level to ${location.getTo: Location}.", location.getTo ) )
  }

  override def onMobilityMoved(mobilityModule: IMobilityModule, location: Change[ITile], remainingSpeed: ChangeDbl) {
    messageBus.publish( new LocatedMessage( s"${mobilityModule.getGameObject.getType.getTypeName} moved to ${location.getTo: Location}.", location.getTo ) )
  }

  override def onConstructorWorked(constructorModule: IConstructorModule, remainingSpeed: ChangeInt) {
    messageBus.publish( new LocatedMessage( s"${constructorModule.getGameObject.getType.getTypeName} is building. ${remainingSpeed.getTo} build units remaining.", constructorModule.getGameObject.checkLocation().get() ) )
  }

  override def onConstructorTargeted(constructorModule: IConstructorModule, target: Change[IGameObject]) {
    messageBus.publish( new LocatedMessage( s"${constructorModule.getGameObject.getType.getTypeName} is now building on ${target.getTo.getType.getTypeName}.", target.getTo.checkLocation().get ) )
  }

  override def onConstructionSiteWorked(constructionSite: IConstructionSite, moduleType: PublicModuleType[_], remainingWork: ChangeInt): Unit = {
    messageBus.publish( new LocatedMessage( s"Construction site at ${(constructionSite.checkLocation: Maybe[_ <: ITile]) match {
      case Present( l ) => l: Location
      case Absent => "<missing>"
      case Unknown => "<unknown>"
    }} only requires ${remainingWork.getTo} build units.", constructionSite.checkLocation().get ) )
  }

  override def onWeaponFired(weaponModule: IWeaponModule, target: ITile, repeated: ChangeInt, ammunition: ChangeInt) {
    messageBus.publish( new LocatedMessage( s"Weapon ${weaponModule} fired site at ${target: Location}.", target ) )
  }

  override def onGameStarted(game: IGame) {
    messageBus.publish( new PlainMessage( s"Game started." ) )
  }

  override def onGameEnded(game: IGame, victoryCondition: PublicVictoryConditionType, victor: IPlayer) {
    messageBus.publish( new PlainMessage( s"Game ended, ${victor.getName} (${victor.getPlayerID}) won." ) )
  }

}
