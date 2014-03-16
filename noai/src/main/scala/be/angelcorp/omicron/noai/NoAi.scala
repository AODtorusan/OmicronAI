package be.angelcorp.omicron.noai

import scala.collection.mutable
import scala.collection.JavaConverters._
import akka.actor.{Props, ActorRef, ActorSystem}
import akka.pattern.ask
import de.lessvoid.nifty.Nifty
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.GameListener
import com.lyndir.omicron.api.model._
import be.angelcorp.omicron.base.{Present, Location}
import be.angelcorp.omicron.base.ai.{AIBuilder, AI}
import be.angelcorp.omicron.base.ai.actions.{Action, ActionExecutor}
import be.angelcorp.omicron.base.bridge._
import be.angelcorp.omicron.base.gui.ActiveGameMode
import be.angelcorp.omicron.base.world._
import be.angelcorp.omicron.base.configuration.Configuration.config
import be.angelcorp.omicron.noai.gui.{GuiController, NoAiGui}
import scala.concurrent.Await
import be.angelcorp.omicron.base.Conversions._
import be.angelcorp.omicron.base.bridge.PlayerGainedObject
import be.angelcorp.omicron.base.util.GenericEventBus
import be.angelcorp.omicron.base.bridge.PlayerGainedObject
import be.angelcorp.omicron.base.Present
import be.angelcorp.omicron.base.world.LocationState
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class NoAi( val actorSystem: ActorSystem, playerId: Int, key: PlayerKey, name: String, color: Color ) extends AI( playerId, key, name, color, color ) with ActionExecutor {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  protected[noai] val getAuth = auth

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val assetListUpdater = new GameListener {
    override def onPlayerGainedObject(player: IPlayer, gameObject: IGameObject): Unit = {
      if (player == NoAi.this) {
        logger.debug(s"New unit: $gameObject")
        units_ += new AssetImpl( NoAi.this.auth, gameObject )
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

  override def prepare(): Unit = auth {
    world = actorSystem.actorOf( World(auth, gameSize) )
    val bridge = actorSystem.actorOf(Props(classOf[GameListenerBridge], auth, gameController), name = "GameListenerBridge")
    gameController.addGameListener( assetListUpdater )

    // Wait for the actors be become live
    Await.result(actorSystem.actorSelection(bridge.path).resolveOne(), timeout.duration)
    Await.result(actorSystem.actorSelection(world.path ).resolveOne(), timeout.duration)

    getController.iterateObservableObjects().asScala.foreach( obj => {
      toMaybe( obj.checkOwner() ) match {
        case Present( owner ) =>
          if (owner == this) units_ += new AssetImpl(auth, obj)
          actorSystem.eventStream.publish( PlayerGainedObject( owner, obj ) )
        case _ =>
      }
    })
  }

  def buildGuiInterface(gui: ActiveGameMode, guiBus: GenericEventBus, nifty: Nifty) = auth {
    new GuiController(this, gui, guiBus, nifty)
  }

  private def gameController = getController.getGameController

  implicit lazy val game = getController.getGameController.getGame
  protected[noai] lazy val gameSize: WorldBounds = game.getLevelSize

  private val units_ = mutable.ListBuffer[Asset]()

  protected[noai] def endTurn(): Unit =
    auth { gameController.setReady() }

  protected[noai] def unitOn(l: Location) = {
    units_.find( _.location.get == l ).orElse( {
      val futureAsset = for (state <- ask(world, LocationState(l)).mapTo[WorldState]) yield state match {
        case KnownState(_, asset, _) =>
          asset
        case _ => None
      }
      Await.result(futureAsset, Duration(1, TimeUnit.MINUTES) )
    } )
  }

  protected[noai] def units =
    units_.result()

  protected[noai] def execute( plan: Action ) =
    for( result <- plan.execute(this) ) result.map( err => {
        lazy val msg = s"Could not finish action $plan successfully: ${err.getMessage}"
        if (err.getCause != null && err.getCause.isInstanceOf[RuntimeException] )
          logger.warn(msg, err )
        else
          logger.info(msg)
        plan.recover( err )
    } )

}

object NoAi extends AIBuilder {

  def apply( actorSystem: ActorSystem, key: PlayerKey, builder: Game.Builder) =
    new NoAi( actorSystem, builder.nextPlayerID, key, config.ai.name, Color.Template.RED.get )

  def apply( actorSystem: ActorSystem, key: PlayerKey, name: String, color: Color, builder: Game.Builder) =
    new NoAi( actorSystem, builder.nextPlayerID, key, name, color )

}
