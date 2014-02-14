package be.angelcorp.omicronai.ai.noai

import scala.collection.JavaConverters._
import scala.collection.mutable
import akka.actor.{ActorRef, Props, ActorSystem}
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.Nifty
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.Color.Template._
import com.lyndir.omicron.api.GameListener
import be.angelcorp.omicronai.configuration.Configuration._
import be.angelcorp.omicronai.ai.AI
import be.angelcorp.omicronai.ai.noai.gui.NoAiGui
import be.angelcorp.omicronai.gui._
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.ai.actions.{ActionExecutor, Action}
import be.angelcorp.omicronai.world.{WorldBounds, World}
import be.angelcorp.omicronai.bridge.{GameListenerBridge, PlayerGainedObject, Asset, AssetImpl}

class NoAi( val actorSystem: ActorSystem, playerId: Int, key: PlayerKey, name: String, color: Color ) extends AI( playerId, key, name, color, color ) with ActionExecutor {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  protected val player    = this
  protected val playerKey = key

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val assetListUpdater = new GameListener {
    override def onPlayerGainedObject(player: IPlayer, gameObject: IGameObject): Unit = {
      if (player == NoAi.this) {
        logger.debug(s"New unit: $gameObject")
        units_ += new AssetImpl( NoAi.this, NoAi.this.key, gameObject )
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

  var world: ActorRef = null

  override def prepare(): Unit = withSecurity(key) {
    world = actorSystem.actorOf( World(this, key, gameSize) )
    actorSystem.actorOf(Props(classOf[GameListenerBridge], this -> key, gameController), name = "GameListenerBridge")
    gameController.addGameListener( assetListUpdater )
    Thread.sleep(200) // Wait for the actors be become live
    getController.listObjects().asScala.foreach( obj => {
      units_ += new AssetImpl(this, key, obj)
      actorSystem.eventStream.publish( PlayerGainedObject( this, obj ) )
    })
  }

  def buildGuiInterface(gui: AiGuiOverlay, nifty: Nifty) = withSecurity(key) {
    new NoAiGui(this, gui, nifty)
  }

  private def gameController = getController.getGameController

  implicit lazy val game = getController.getGameController.getGame
  protected[noai] lazy val gameSize: WorldBounds = game.getLevelSize

  private val units_ = mutable.ListBuffer[Asset]()

  protected[noai] val _plannedActions = mutable.Map[Asset, Option[Action]]()
  protected[noai] def plannedAction = (selected flatMap _plannedActions.get).flatten

  protected[noai] var _selected: Option[Asset] = None
  protected[noai] def selected = _selected

  protected[noai] def endTurn(): Unit =
    withSecurity(key) { gameController.setReady() }

  protected[noai] def select( asset: Asset): Unit =
    _selected = Some(asset)

  protected[noai] def selectNext(): Unit = {
    _selected match {
      case Some(unit) =>
        val idx = (units_.indexOf( unit ) + 1) % units.size
        select( units_(idx) )
      case None =>
        units_.headOption.map( select )
    }
  }

  protected[noai] def selectPrevious(): Unit = {
    _selected match {
      case Some(unit) =>
        val idx = (units_.indexOf( unit ) - 1 + units.size) % units.size
        select( units_(idx) )
      case None =>
        units_.headOption.map( select )
    }
  }

  protected[noai] def unitOn(l: Location) =
    units_.find( _.location == l )

  protected[noai] def units =
    units_.result()

  protected[noai] def updateOrConfirmAction( action: Action) =
    plannedAction match {
      // Execute the plan (the same plan was passed in)
      case Some(plan) if plan == action =>
        for( result <- plan.execute(this) ) result match {
          case Some( err ) =>
            lazy val msg = s"Could not finish action $plan successfully: ${err.getMessage}"
            if (err.getCause != null && err.getCause.isInstanceOf[RuntimeException] )
              logger.warn(msg, err )
            else
              logger.info(msg)

            _plannedActions.update( selected.get, plan.recover( err ))
          case None =>
            logger.info(s"Action $plan finished successfully")
            _plannedActions.update( selected.get, None )
        }
      // Update the plan
      case _ =>
        _plannedActions.update( selected.get, Some(action))
    }

}

object NoAi {

  def apply( actorSystem: ActorSystem, key: PlayerKey, builder: Game.Builder) =
    new NoAi( actorSystem, builder.nextPlayerID, key, config.ai.name, RED.get )

}
