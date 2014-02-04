package be.angelcorp.omicronai.gui.layerRender

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.mutable
import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import org.newdawn.slick.{Color, Graphics}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.ResourceType
import com.lyndir.omicron.api.model.ResourceType._
import be.angelcorp.omicronai.HexTile
import be.angelcorp.omicronai.gui.{Canvas, ViewPort}
import be.angelcorp.omicronai.configuration.Configuration._
import be.angelcorp.omicronai.world.{GhostState, KnownState, WorldState, LocationStates}

class ResourceRenderer(val world: ActorRef) extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit val timeout: Timeout = config.ai.messageTimeout seconds;

  def render(g: Graphics, view: ViewPort) {
    import scala.concurrent.ExecutionContext.Implicits.global
    val futureResources = (world ? LocationStates(view.tilesInView.toSeq)).mapTo[Seq[WorldState]]
    val allResources = Await.result( futureResources, timeout.duration).zip(view.tilesInView)

    val filledTiles = mutable.Map[ResourceType, mutable.ListBuffer[HexTile]]()
    val tiles = allResources.map( {
      case (KnownState(_,_,res), loc) =>
        for ((typ, count)<-res)
          if (count > 0) filledTiles.getOrElseUpdate(typ, mutable.ListBuffer()) += loc
        HexTile(loc) -> Some( res )
      case (GhostState(_,_,res), loc) =>
        for ((typ, count)<-res)
          if (count > 0) filledTiles.getOrElseUpdate(typ, mutable.ListBuffer()) += loc
        HexTile(loc) -> Some( res )
      case (_, loc)                   =>
        HexTile(loc) -> None
    } )

    for ((typ, tiles) <- filledTiles) typ match {
      case FUEL          => Canvas.render(g, tiles, Color.transparent, new Color(  0, 255,   0, 128))
      case SILICON       => Canvas.render(g, tiles, Color.transparent, new Color(255, 255,   0, 128))
      case METALS        => Canvas.render(g, tiles, Color.transparent, new Color(128, 128, 128, 128))
      case RARE_ELEMENTS => Canvas.render(g, tiles, Color.transparent, new Color(  0,   0, 255, 128))
      case r => logger.warn(s"Detected unknown resource type for ResourceRenderer: $r")
    }

    for ( (tile, res) <- tiles ) {
      val str = res match {
        case Some( resources ) =>
          val fuel      = resources.getOrElse(FUEL,          0)
          val silicon   = resources.getOrElse(SILICON,       0)
          val metal     = resources.getOrElse(METALS,        0)
          val rare_elem = resources.getOrElse(RARE_ELEMENTS, 0)
          s"$fuel $silicon\n$metal $rare_elem"
        case _ => "? ?\n? ?"
      }
      Canvas.text(g, tile, str )
    }
  }

  override val toString =
    s"Detected and estimated resources"
}

