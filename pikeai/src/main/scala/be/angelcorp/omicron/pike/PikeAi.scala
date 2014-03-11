package be.angelcorp.omicron.pike

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
import be.angelcorp.omicron.base.Conversions._
import be.angelcorp.omicron.base.ai.{AIBuilder, AI}
import be.angelcorp.omicron.base.bridge.{GameListenerBridge, PlayerGainedObject}
import be.angelcorp.omicron.base.configuration.Configuration.config
import be.angelcorp.omicron.base.gui.{GuiInterface, AiGuiOverlay}
import be.angelcorp.omicron.base.gui.layerRender.LayerRenderer
import be.angelcorp.omicron.pike.agents.Admiral
import be.angelcorp.omicron.pike.supervisor.GuiSupervisor
import be.angelcorp.omicron.pike.gui._
import be.angelcorp.omicron.pike.agents.Self
import be.angelcorp.omicron.base.Present
import be.angelcorp.omicron.base.util.GenericEventBus

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
  //AiSupervisor.omicron.base = Some(supervisorRef)

  def buildGuiInterface(gui: AiGuiOverlay, guiBus: GenericEventBus, nifty: Nifty) = {
    new PikeInterface(this, gui, guiBus, nifty)
  }

  override def prepare() = auth {
    admiralRef = actorSystem.actorOf(Props(classOf[Admiral], this, key), name = "AdmiralPike")
    val bridge = actorSystem.actorOf(Props(classOf[GameListenerBridge], this -> key, getController.getGameController), name = "GameListenerBridge")
    actorSystem.eventStream.setLogLevel(Logging.DebugLevel)

    // Wait for the actors to become live
    Await.result(actorSystem.actorSelection(bridge.path).resolveOne(), timeout.duration)
    Await.result(actorSystem.actorSelection(world.path ).resolveOne(), timeout.duration)

    getController.iterateObservableObjects().asScala.foreach( obj => {
      toMaybe( obj.checkOwner() ) match {
        case Present( owner ) => actorSystem.eventStream.publish( PlayerGainedObject( owner, obj ) )
        case _ =>
      }
    })
  }

}

object PikeAi extends AIBuilder {

  def apply( actorSystem: ActorSystem, key: PlayerKey, builder: Game.Builder) =
    new PikeAi( actorSystem, builder.nextPlayerID, key, config.ai.name, RED.get )

  def apply( actorSystem: ActorSystem, key: PlayerKey, name: String, color: Color, builder: Game.Builder) =
    new PikeAi( actorSystem, builder.nextPlayerID, key, name, color )

}

class PikeInterface(val pike: PikeAi, val gui: AiGuiOverlay, val guiBus: GenericEventBus, val nifty: Nifty) extends GuiInterface {
  nifty.addScreen( PikeUserInterface.screenId, PikeUserInterface.screen(nifty, gui) )

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

  nifty.gotoScreen( PikeUserInterface.screenId )

  def updateUI() {
    controllers.foreach( _.updateUI() )
  }

}
