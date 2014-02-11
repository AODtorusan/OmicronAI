package be.angelcorp.omicronai.gui

import java.util.concurrent.LinkedBlockingDeque
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import akka.actor.Props
import org.slf4j.LoggerFactory
import org.newdawn.slick._
import org.newdawn.slick.state.GameState
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.Game
import de.lessvoid.nifty.slick2d.{NiftyOverlayBasicGameState, NiftyStateBasedGame, NiftyOverlayGameState}
import be.angelcorp.omicronai._
import be.angelcorp.omicronai.ai.pike.PikeAi
import be.angelcorp.omicronai.ai.lance.LanceAi
import be.angelcorp.omicronai.ai.AI
import be.angelcorp.omicronai.ai.pike.agents.Admiral
import be.angelcorp.omicronai.ai.noai.NoAi
import be.angelcorp.omicronai.configuration.Configuration.config
import be.angelcorp.omicronai.gui.textures.Textures

class AiGui extends NiftyStateBasedGame("Omicron AI gui") with ExecutionContext {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  val splash = new SplashOverlay()

  val workList = new LinkedBlockingDeque[Runnable]()

  val loadThread = new Thread {
    override def run() {
      implicit val openglContext: ExecutionContext = AiGui.this

      splash.progress(0.1f, "Building game ...")
      logger.info("Building game framework")
      val builder = Game.builder
      builder.addGameListener(new GameListenerLogger)
      logger.info("Game framework build!")

      splash.progress(0.2f, "Building AI ...")
      logger.info("Building new ai")
      val ai: AI = config.ai.engine match {
        case "PikeAI"  => new PikeAi( (player, system) => system.actorOf(Props(new Admiral(player)), name = "AdmiralPike"), builder )
        case "LanceAI" => new LanceAi( builder )
        case _ => new NoAi( builder )
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

      splash.progress(0.6f, "Loading unit/texture configuration ...")
      logger.info("Building ...")
      Textures.load()
      logger.info("Unit/texture configurations build!")

      splash.progress(0.8f, "Finalizing GUI ...")
      logger.info("Building AI gui")
      val aiGui = new AiGuiOverlay(game, ai)
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
    loadThread.start()
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
