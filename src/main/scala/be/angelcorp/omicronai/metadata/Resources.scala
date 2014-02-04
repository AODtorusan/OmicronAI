package be.angelcorp.omicronai.metadata

import akka.actor.ActorRef
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicronai.gui.layerRender.{ResourceRenderer, LayerRenderer}

class Resources(val world: ActorRef) extends MetaData {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def title = "Resource layers"

  def layers: Map[String, LayerRenderer] = Map(
    "Detected and estimated resources" -> new ResourceRenderer(world)
  )

}
