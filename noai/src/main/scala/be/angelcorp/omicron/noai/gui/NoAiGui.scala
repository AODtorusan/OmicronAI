package be.angelcorp.omicron.noai.gui

import scala.Some
import scala.collection.mutable
import akka.actor.{ActorRef, Actor, Props}
import akka.event.{SubchannelClassification, EventBus}
import akka.util.Subclassification
import de.lessvoid.nifty.Nifty
import org.newdawn.slick.{Graphics, Color}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.LevelType
import be.angelcorp.omicron.base.{Present, HexTile, Location}
import be.angelcorp.omicron.base.bridge.Asset
import be.angelcorp.omicron.base.gui._
import be.angelcorp.omicron.base.gui.layerRender._
import be.angelcorp.omicron.base.gui.slick.DrawStyle
import be.angelcorp.omicron.base.world.{GhostState, KnownState, SubWorld}
import be.angelcorp.omicron.base.Conversions._
import be.angelcorp.omicron.noai.{GuiMessage, NoAiGameListener, NoAi}
import be.angelcorp.omicron.noai.gui.screens.{NoAiUserInterfaceController, NoAiConstructionScreenController}
import be.angelcorp.omicron.base.gui.layerRender.renderEngine.RenderEngine
import be.angelcorp.omicron.base.world.GhostState
import be.angelcorp.omicron.base.world.SubWorld
import scala.Some
import be.angelcorp.omicron.base.world.KnownState

class NoAiGui(val noai: NoAi, val frame: AiGuiOverlay, val nifty: Nifty) extends NiftyGuiInterface {
  private val logger = Logger( LoggerFactory.getLogger( getClass ) )
  val listener = new NoAiGameListener( this )
  frame.game.getController.addGameListener( listener )
  noai.actorSystem.actorOf( Props(classOf[NoAiInput], noai, this), name = "NoAI_input" )

  val guiMessageBus = new EventBus with SubchannelClassification {
    type Event = AnyRef
    type Classifier = Class[_]
    type Subscriber = ActorRef
    override protected implicit val subclassification = new Subclassification[Class[_]] {
      def isEqual(x: Class[_], y: Class[_]) = x == y
      def isSubclass(x: Class[_], y: Class[_]) = y isAssignableFrom x
    }
    override protected def classify(event: AnyRef): Class[_] = event.getClass
    override protected def publish(event: AnyRef, subscriber: ActorRef): Unit = subscriber ! event
  }

  private val uiScreen = screens.NoAiUserInterface.screen(this)
  private val constructionScreen = screens.NoAiConstructionScreen.screen(this)
  private val messagesScreen = screens.NoAiMessagesScreen.screen(this)
  nifty.addScreen( uiScreen.getScreenId, uiScreen )
  nifty.addScreen( constructionScreen.getScreenId, constructionScreen )
  nifty.addScreen( messagesScreen.getScreenId, messagesScreen )
  nifty.gotoScreen( uiScreen.getScreenId )

  private val messages = mutable.ListBuffer[String]()
  private val messageLabel = uiScreen.findNiftyControl("messages", classOf[de.lessvoid.nifty.controls.Label])

  noai.actorSystem.actorOf( Props( new Actor {
    override def preStart() {
      guiMessageBus.subscribe(context.self, classOf[GuiMessage])
    }
    override def receive = {
      case m: GuiMessage =>
        messages.prepend( m.message )
        if (messages.size > 10) messages.remove(messages.size - 1)
        messageLabel.setText( messages.mkString("\n") )
    }
  } ) )

  private val gridRenderer      = new GridRenderer(noai)
  private val resourceRenderer  = new ResourceRenderer(noai.world)

  protected[gui] var gridOn       = true
  protected[gui] var resourcesOn  = false

  private val staticLayers = mutable.ListBuffer[ LayerRenderer ]()
  staticLayers += new RenderEngine()
  staticLayers += new LayerRenderer {
    val unknown = new DrawStyle(Color.white, 3.0f)
    var tiles: Map[DrawStyle, Iterable[HexTile]] = Map.empty
    override def prepareRender(subWorld: SubWorld, layer: Int) = {
      tiles = subWorld.states.flatten.flatMap {
        case (loc, KnownState(_,Some(obj),_)) =>
          Some(HexTile(loc) -> ( obj.owner match {
            case Some( owner ) => DrawStyle(owner.getPrimaryColor, 3.0f)
            case _ => unknown
          } ) )
        case (loc, GhostState(_,Some(obj),_)) =>
          Some(HexTile(loc) -> ( obj.owner match {
            case Some( owner ) => DrawStyle(owner.getPrimaryColor, 3.0f)
            case _ => unknown
          } ) )
        case _ => None
      }.toList.groupBy( _._2 ).mapValues( _.map( _._1 ) )
    }
    override def render(g: Graphics) = {
      for ((color, locations) <- tiles)
        Canvas.render(g, locations, color)

    }
  }
  staticLayers += new LayerRenderer {
    // Renders the currently selected unit
    override def prepareRender(subWorld: SubWorld, layer: Int) {}
    override def render(g: Graphics) {
      noai.selected match {
        case Some( unit ) =>
          Canvas.render(g, unit.location.get, new DrawStyle(Color.orange, 3.0f), Color.transparent)
        case _ =>
      }
    }
  }
  staticLayers += new FieldOfView( noai.world )

  new Thread {
    override def run() {
      // TODO: Fix this quickfix : )
      Thread.sleep(1000)
      frame.view.centerOn( noai.units.head.location.get )
    }
  }.start()

  def activeLayers: Seq[LayerRenderer] = if (hideGame) Nil else {
    val layers = mutable.ListBuffer[LayerRenderer]( staticLayers: _* )
    if (gridOn) layers += gridRenderer
    noai.plannedAction.foreach( plan => layers += plan.preview )
    if (resourcesOn) layers += resourceRenderer
    layers
  }

  def moveTo(asset: Asset) {
    val loc = asset.location.get
    moveTo( loc.h )
    frame.view.centerOn( loc )
  }

  def moveTo(h: Int) {
    frame.view.activeLayer = h
    uiScreen.getScreenController.asInstanceOf[NoAiUserInterfaceController].sidebarController.layerLabel.setText(
      LevelType.values()(h).getName
    )
  }

  def moveUp()   = moveTo( math.min( frame.view.activeLayer + 1, noai.gameSize.hSize - 1) )
  def moveDown() = moveTo( math.max( frame.view.activeLayer - 1, 0 ) )

}
