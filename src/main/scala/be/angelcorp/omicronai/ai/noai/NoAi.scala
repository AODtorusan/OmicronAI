package be.angelcorp.omicronai.ai.noai

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.Nifty
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.Color.Template._
import com.lyndir.omicron.api.GameListener
import be.angelcorp.omicronai.configuration.Configuration
import Configuration._
import be.angelcorp.omicronai.ai.AI
import be.angelcorp.omicronai.ai.noai.gui.NoAiGui
import be.angelcorp.omicronai.gui._
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.ai.noai.actions.NoAiAction
import be.angelcorp.omicronai.world.{WorldSize, World, WorldInterface}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class NoAi( playerId: Int, key: PlayerKey, name: String, color: Color ) extends AI( playerId, key, name, color, color ) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  Security.authenticate(this, key)

  def this( builder: Game.Builder) =
    this( builder.nextPlayerID, new PlayerKey, config.ai.name, RED.get )

  val assetListUpdater = new GameListener {
    override def onPlayerGainedObject(player: IPlayer, gameObject: IGameObject): Unit = {
      if (player == NoAi.this) {
        logger.debug(s"New unit: $gameObject")
        units_ += new Asset( NoAi.this, gameObject )
      }
    }
    override def onPlayerLostObject(player: IPlayer, gameObject: IGameObject): Unit = {
      if (player == NoAi.this) {
        logger.debug(s"Lost unit: $gameObject")
        val idx = units_.indexWhere( _.gameObject == gameObject )
        if (idx != -1) units_.remove( idx )
        else logger.warn(s"An own unit was destroyed, but it was not being tracked: $gameObject")
      }
    }
  }

  lazy val world = World.withInterface(this, gameSize)

  override def start(): Unit = {
    getController.listObjects().asScala.foreach( obj => units_ += new Asset(this, obj) )
    gameController.addGameListener( assetListUpdater )
    gameController.addGameListener(  world.listener )
    super.start()
  }

  def buildGuiInterface(gui: AiGuiOverlay, nifty: Nifty) = {
    Security.authenticate(this, key)
    new NoAiGui(this, gui, nifty)
  }

  private def gameController = getController.getGameController

  private[noai] implicit lazy val game = getController.getGameController.getGame
  private[noai] lazy val gameSize: WorldSize = game.getLevelSize

  private val units_ = ListBuffer[Asset]()

  protected[noai] var _plannedAction: Option[NoAiAction] = None
  protected[noai] def plannedAction = _plannedAction

  protected[noai] var _selected: Option[Asset] = None
  protected[noai] def selected = _selected


  protected[noai] def attack( asset: Asset, weaponModule: WeaponModule, target: Location): Boolean = {
    if (!units_.contains(asset)) {
      logger.warn(s"Tried to attack with unit $asset, but that unit is not owned by the noai instance"); false
    } else {
      asset.weapons.find( _ == weaponModule ) match {
        case Some(module) =>
          val success = module.fireAt( target )
          logger.trace( if (success) s"$asset shot at $target with $module" else s"Asset $asset could not successfully fire at $target with $module")
          success
        case None =>
          logger.warn(s"Could not find weapon module $weaponModule on unit $asset"); false
      }
    }
  }

  protected[noai] def endTurn(): Unit =
    gameController.setReady()

  protected[noai] def move( asset: Asset, path: Seq[Location]): Unit = {
    if (!units_.contains(asset))
      logger.warn(s"Tried to move unit $asset, but that unit is not owned by the noai instance")
    else if (asset.location != path.head)
      logger.warn(s"Tried to move unit $asset, but the planned path does not start at the asset location: $path")
    else {
      asset.mobility match {
        case Some(module) =>
          path.drop(1).foreach( target => {
            waitForWorld()
            val action = module.movement( target )
            if (action.isPossible) {
              logger.trace(s"Moving asset $asset to $target")
              action.execute()
            } else
              logger.trace(s"Cannot move asset $asset to $target")
          } )
        case None =>
          logger.warn(s"Tried to move unit $asset, but this unit cannot move (no mobility module)")
      }
    }
  }

  protected[noai] def select( asset: Asset): Unit = {
    _selected = Some(asset)
    _plannedAction = None
  }

  protected[noai] def unitOn(l: Location) =
    units_.find( _.location == l )

  protected[noai] def units =
    units_.result()

  protected[noai] def updateOrConfirmAction( action: NoAiAction) =
    plannedAction match {
      case Some(plan) if plan == action => _plannedAction = plan.execute(this)
      case _ => _plannedAction = Some(action)
    }

  protected[noai] def waitForWorld() {
    Await.result( world.isReady, Duration(1, TimeUnit.MINUTES))
  }

}

