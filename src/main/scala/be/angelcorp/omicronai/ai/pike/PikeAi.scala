package be.angelcorp.omicronai.ai.pike

import scala.Some
import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.{Props, ActorRef, ActorSystem}
import akka.event.Logging
import akka.util.Timeout
import akka.pattern.ask
import de.lessvoid.nifty.{NiftyEventAnnotationProcessor, Nifty}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.Color.Template._
import be.angelcorp.omicronai.AiSupervisor
import be.angelcorp.omicronai.ai.AI
import be.angelcorp.omicronai.ai.pike.agents.{Admiral, Self}
import be.angelcorp.omicronai.configuration.Configuration.config
import be.angelcorp.omicronai.gui._
import be.angelcorp.omicronai.gui.screens.ui.pike._
import be.angelcorp.omicronai.gui.layerRender.LayerRenderer
import be.angelcorp.omicronai.bridge.{GameListenerBridge, PlayerGainedObject}
import be.angelcorp.omicronai.world.World


class PikeAi( val actorSystem: ActorSystem, playerId: Int, key: PlayerKey, name: String, color: Color) extends AI( playerId, key, name, color, color ) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit def timeout: Timeout = config.ai.messageTimeout seconds;

  implicit val context = actorSystem.dispatcher

  var admiralRef: ActorRef = null

  lazy val world =
    Await.result( actorSystem.actorSelection(admiralRef.path / "World").resolveOne, timeout.duration )

  lazy val supervisorRef =
    actorSystem.actorOf( Props( new GuiSupervisor(admiralRef, this) ), name="AiSupervisor" )

  var supervisor = {
    implicit val timeout: Timeout = 5 seconds;
    Await.result( supervisorRef ? Self(), timeout.duration ).asInstanceOf[GuiSupervisor]
  }
  //AiSupervisor.supervisor = Some(supervisorRef)

  def buildGuiInterface(gui: AiGuiOverlay, nifty: Nifty) = {
    new PikeInterface(this, gui, nifty)
  }

  override def prepare() = withSecurity(key) {
    admiralRef = actorSystem.actorOf(Props(classOf[Admiral], this), name = "AdmiralPike")
    actorSystem.actorOf(Props(classOf[GameListenerBridge], this -> getKey, getController.getGameController), name = "GameListenerBridge")
    actorSystem.eventStream.setLogLevel(Logging.DebugLevel)

    Thread.sleep(200)
    getController.listObjects().asScala.foreach( obj => {
      actorSystem.eventStream.publish( PlayerGainedObject( this, obj ) )
    })
  }

}

object PikeAi {

  def apply( actorSystem: ActorSystem, key: PlayerKey, builder: Game.Builder) =
    new PikeAi( actorSystem, builder.nextPlayerID, key, config.ai.name, RED.get )

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
