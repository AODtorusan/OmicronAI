package be.angelcorp.omicronai.ai.pike

import scala.Some
import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.{Props, ActorRef, ActorSystem}
import de.lessvoid.nifty.{NiftyEventAnnotationProcessor, Nifty}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.Color.Template._
import be.angelcorp.omicronai.ai.AI
import be.angelcorp.omicronai.configuration.Configuration.config
import be.angelcorp.omicronai.gui._
import be.angelcorp.omicronai.ai.pike.agents.{ListMembers, PlayerGainedObject, Admiral, Self}
import be.angelcorp.omicronai.{Location, AiSupervisor}
import be.angelcorp.omicronai.gui.screens.ui.pike._
import be.angelcorp.omicronai.gui.layerRender.LayerRenderer
import be.angelcorp.omicronai.world.{ReloadLocation, WorldUpdater}
import com.lyndir.omicron.api.util.Maybe.Presence
import akka.event.Logging


class PikeAi( aiBuilder: (PikeAi, ActorSystem) => ActorRef, playerId: Int, key: PlayerKey, name: String, color: Color ) extends AI( playerId, key, name, color, color ) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit def timeout: Timeout = config.ai.messageTimeout seconds;

  Security.authenticate(this, key)
  val actorSystem = ActorSystem()
  implicit val context = actorSystem.dispatcher

  def this( aiBuilder: (PikeAi, ActorSystem) => ActorRef, builder: Game.Builder) =
    this( aiBuilder, builder.nextPlayerID, new PlayerKey, config.ai.name, RED.get )

  lazy val admiralRef = {
    logger.info(s"Building AI logic for AI player ${getName}")
    aiBuilder(this, actorSystem)
  }
  lazy val admiral = {
    Await.result( admiralRef ? Self(), timeout.duration ).asInstanceOf[Admiral]
  }
  def world = admiral.world

  val supervisorRef = actorSystem.actorOf( Props( new GuiSupervisor(admiralRef, this) ), name="AiSupervisor" )
  var supervisor = {
    implicit val timeout: Timeout = 5 seconds;
    Await.result( supervisorRef ? Self(), timeout.duration ).asInstanceOf[GuiSupervisor]
  }
  AiSupervisor.supervisor = Some(supervisorRef)

  def buildGuiInterface(gui: AiGuiOverlay, nifty: Nifty) = {
    Security.authenticate(this, key)
    new PikeInterface(this, gui, nifty)
  }

  override def start() {
    Security.authenticate(this, key)
    getController.getGameController.addGameListener( admiral.messageListener )
    actorSystem.eventStream.setLogLevel(Logging.DebugLevel)
    getController.listObjects().asScala.foreach( obj => {
      actorSystem.eventStream.publish( PlayerGainedObject( this, obj ) )
    })
    getController.getGameController.setReady()
  }
}

class PikeInterface(val pike: PikeAi, val gui: AiGuiOverlay, val nifty: Nifty) extends GuiInterface {
  nifty.addScreen( screens.Introduction.name,     screens.Introduction.screen(nifty, gui)     )
  nifty.addScreen( screens.ui.UserInterface.name, screens.ui.pike.PikeUserInterface.screen(nifty, gui) )

  val activeLayers = mutable.ListBuffer[ LayerRenderer ]()

  val lc  = new LayerController(this)
  val utc = new UnitTreeController(this)
  val uic = new UserInterfaceController(this, utc)
  val mtc = new MessageTabController(this, utc)
  val ptc = new ProbeTabController(this, utc)

  val controllers = List(lc, utc, uic, mtc, ptc)
  for (controller <- controllers) {
    NiftyEventAnnotationProcessor.process( controller )
    controller.populate()
  }

  nifty.gotoScreen( screens.Introduction.name )

  def updateUI() {
    controllers.foreach( _.updateUI() )
  }

}
