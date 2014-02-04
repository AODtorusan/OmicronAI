package be.angelcorp.omicronai.metadata

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import org.newdawn.slick.{Color, Graphics}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.ResourceType
import be.angelcorp.omicronai.configuration.Configuration
import Configuration._
import be.angelcorp.omicronai.ai.pike.agents._
import be.angelcorp.omicronai.gui.layerRender.LayerRenderer
import be.angelcorp.omicronai.gui.{ViewPort, Canvas}

class Resources(val cartographer: ActorRef) extends MetaData {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def title = "Resource layers"

  def layers: Map[String, LayerRenderer] = Map(
    "Detected and estimated fuel"          -> new ResourceRenderer(cartographer, ResourceType.FUEL),
    "Detected and estimated silicom"       -> new ResourceRenderer(cartographer, ResourceType.SILICON),
    "Detected and estimated metal"         -> new ResourceRenderer(cartographer, ResourceType.METALS),
    "Detected and estimated rare elements" -> new ResourceRenderer(cartographer, ResourceType.RARE_ELEMENTS)
  )

}

class ResourceRenderer(val cartographer: ActorRef, val resourceType: ResourceType) extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit val timeout: Timeout = config.ai.messageTimeout seconds;

  val tiles = ListBuffer[Canvas]()

  override def update(view: ViewPort) {
    import scala.concurrent.ExecutionContext.Implicits.global
    tiles.clear()
    val futureResources = Future.sequence( view.tilesInView.map( tile => {
      cartographer ? ResourcesOn( tile, resourceType )
    } ) )
    val resources = Await.result( futureResources, timeout.duration).map( _.asInstanceOf[ResourceCount] )
    tiles.appendAll( resources.map( c => {
      new Canvas( c.l ) {
        override def fillColor   = if (c.quantity > 0.0 )   new Color(0f, 0.5f, 0f, 1.0f)                 else Color.transparent
        override def borderStyle = if (c.confidence != 0.0) new Color(0f, 0.5f, 0f, c.confidence.toFloat) else Color.transparent
        override def textColor   = Color.white
        override def text        = (c.quantity, c.confidence).toString()
      }
    } ) )
  }

  def render(g: Graphics, view: ViewPort) { tiles.foreach( _.render(g) ) }

  override val toString =
    s"Detected and estimated ${resourceType.name()}"
}
