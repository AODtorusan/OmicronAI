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
import be.angelcorp.omicronai.ai.{ActionExecutor, AI}
import be.angelcorp.omicronai.ai.noai.gui.NoAiGui
import be.angelcorp.omicronai.gui._
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.assets.{AssetImpl, Asset}
import be.angelcorp.omicronai.ai.actions.Action
import be.angelcorp.omicronai.world.{WorldUpdater, WorldSize, World, WorldInterface}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import akka.actor.ActorSystem

class NoAi( playerId: Int, key: PlayerKey, name: String, color: Color ) extends AI( playerId, key, name, color, color ) with ActionExecutor {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  Security.authenticate(this, key)

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  def this( builder: Game.Builder) =
    this( builder.nextPlayerID, new PlayerKey, config.ai.name, RED.get )

  val assetListUpdater = new GameListener {
    override def onPlayerGainedObject(player: IPlayer, gameObject: IGameObject): Unit = {
      if (player == NoAi.this) {
        logger.debug(s"New unit: $gameObject")
        units_ += new AssetImpl( NoAi.this, gameObject )
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

  val actorSystem = ActorSystem("WorldActorSystem")
  lazy val world = actorSystem.actorOf( World(this, gameSize) )

  override def start(): Unit = {
    getController.listObjects().asScala.foreach( obj => units_ += new AssetImpl(this, obj) )
    gameController.addGameListener( assetListUpdater )
    gameController.addGameListener( new WorldUpdater(world) )
    super.start()
  }

  def buildGuiInterface(gui: AiGuiOverlay, nifty: Nifty) = {
    Security.authenticate(this, key)
    new NoAiGui(this, gui, nifty)
  }

  private def gameController = getController.getGameController

  implicit lazy val game = getController.getGameController.getGame
  protected[noai] lazy val gameSize: WorldSize = game.getLevelSize

  private val units_ = ListBuffer[Asset]()

  protected[noai] var _plannedAction: Option[Action] = None
  protected[noai] def plannedAction = _plannedAction

  protected[noai] var _selected: Option[Asset] = None
  protected[noai] def selected = _selected

  protected[noai] def endTurn(): Unit =
    gameController.setReady()

  protected[noai] def select( asset: Asset): Unit = {
    _selected = Some(asset)
    _plannedAction = None
  }

  protected[noai] def unitOn(l: Location) =
    units_.find( _.location == l )

  protected[noai] def units =
    units_.result()

  protected[noai] def updateOrConfirmAction( action: Action) =
    _plannedAction = plannedAction match {
      // Execute the plan (the same plan was passed in)
      case Some(plan) if plan == action =>
        Await.result(
          for( result <- plan.execute(this) ) yield result match {
            case Some( err ) =>
              logger.info(s"Could not finish action $plan successfully: ${err.getMessage}")
              plan.recover( err )
            case None => None
          }, Duration(1, TimeUnit.MINUTES)
        )
      // Update the plan
      case _ =>
        Some(action)
    }

}

