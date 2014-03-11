package be.angelcorp.omicron.gui

import java.util.concurrent.LinkedBlockingDeque
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import akka.actor.ActorSystem
import org.slf4j.LoggerFactory
import org.newdawn.slick._
import org.newdawn.slick.state.GameState
import org.newdawn.slick.util.ResourceLoader
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.{PlayerKey, Game}
import de.lessvoid.nifty.slick2d.{NiftyOverlayGameState, NiftyOverlayBasicGameState, NiftyStateBasedGame}
import be.angelcorp.omicron.base.ai.{AIBuilder, AI}
import be.angelcorp.omicron.base.configuration.Configuration.config
import be.angelcorp.omicron.base.{Auth, GameListenerLogger}
import be.angelcorp.omicron.base.gui.ActiveGameMode
import be.angelcorp.omicron.base.sprites.Sprites


class AiGui extends NiftyStateBasedGame("Omicron AI gui") with ExecutionContext {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  val splash = new SplashMode()

  val workList = new LinkedBlockingDeque[Runnable]()

  def loadThread( container: GameContainer ) = new Thread {
    override def run() {
      implicit val openglContext: ExecutionContext = AiGui.this

      splash.progress(0.1f, "Building actor system ...")
      logger.info("Building actor system")
      val actorSystem = ActorSystem()
      logger.info("Actor system build!")

      splash.progress(0.1f, "Building game ...")
      logger.info("Building game framework")
      val builder = Game.builder
      logger.info("Game framework build!")

      splash.progress(0.2f, "Building AI ...")
      logger.info("Building new ai")
      val key = new PlayerKey

      val ai: AI = {
        val aiClass = Class.forName( config.ai.engine )

        import scala.reflect.runtime._
        val rootMirror = universe.runtimeMirror(getClass.getClassLoader)
        val classSymbol = rootMirror.classSymbol( aiClass )
        val companionSymbol = classSymbol.companionSymbol.asModule
        val companionMirror = rootMirror.reflectModule(companionSymbol)
        val aiBuilder = companionMirror.instance.asInstanceOf[AIBuilder]

        aiBuilder.apply( actorSystem, key, builder )
      }
      Auth.withSecurity(ai, key) {
        builder.addGameListener(new GameListenerLogger)
      }
      builder.addPlayer(ai)
      logger.info("AI build!")

      splash.progress(0.3f, "Starting game ...")
      logger.info("Starting game")
      val game = builder.build
      logger.info("Game started!")

      splash.progress(0.4f, "Starting AI ...")
      logger.info("Starting AI")
      ai.prepare()
      logger.info("AI started!")

      splash.progress(0.6f, "Loading unit/texture sprites ...")
      logger.info("Building ...")
      logger.debug("Loading textures configurations and sprites ...")
      Sprites.load()
      logger.debug("Loading terrain set ...")
      Await.result( Future{
        config.gui.terrainSet
      }, 1 minute )
      logger.debug("Loading unit set ...")
      Await.result( Future{config.gui.unitSet    }, 1 minute )
      logger.info("Unit/texture configurations and sprites build!")

      splash.progress(0.8f, "Finalizing GUI ...")
      logger.info("Building AI gui")
      val aiGui = new ActiveGameMode(game, actorSystem, AiGui.this, ai)
      Await.result( Future{ add(aiGui) }, 1 minute )
      logger.info("AI gui build!")

      splash.progress(1f, "Done preloading!")
      logger.info("Done preloading!")
      Future {
        enterState(aiGui.getID)
      }
    }
  }

  override def initStatesList(container: GameContainer) {
    addState( splash )
    enterState( splash.getID )
    loadThread(container).start()
  }

  override def preUpdateState(container: GameContainer, delta: Int) {
    while( !workList.isEmpty ) {
      val work = workList.takeLast()
      work.run()
    }
  }

  override def reportFailure(t: Throwable) {
    logger.warn("Problem in AiGui GL execution callbacks", t)
  }

  override def execute(runnable: Runnable) {
    workList.push(runnable)
  }


  def add(gameState: GameState): Unit = {
    addState(gameState)
    gameState.init(getContainer, this)
  }

  def add(gameState: NiftyOverlayBasicGameState): Unit = {
    addState(gameState)
    gameState.init(getContainer, this)
  }

  def add(gameState: NiftyOverlayGameState): Unit = {
    addState(gameState)
    gameState.init(getContainer, this)
  }

}

object AiGui {

  def start() {
    val gui = new AiGui

    val g   = config.graphics
    val app = new AppGameContainer(gui, g.width, g.height, g.fullscreen)
    app.setClearEachFrame(false)
    app.setAlwaysRender(g.alwaysRender)
    app.setMultiSample(g.multisampling)
    app.setShowFPS(g.showFPS)
    app.setTargetFrameRate(g.targetFrameRate)
    app.setVSync(g.vSync)
    app.start()
  }

}
