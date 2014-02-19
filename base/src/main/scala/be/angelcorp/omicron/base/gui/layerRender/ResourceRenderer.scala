package be.angelcorp.omicron.base.gui.layerRender

import scala.Some
import scala.collection.mutable
import akka.actor.ActorRef
import org.newdawn.slick.{Color, Graphics}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.ResourceType
import com.lyndir.omicron.api.model.ResourceType._
import be.angelcorp.omicron.base.HexTile
import be.angelcorp.omicron.base.gui.Canvas
import be.angelcorp.omicron.base.world.{GhostState, KnownState, SubWorld}

class ResourceRenderer(val world: ActorRef) extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val filledTiles = mutable.Map[ResourceType, mutable.ListBuffer[HexTile]]()
  var tiles = Array.ofDim[(HexTile, Option[Map[ResourceType, Int]])](0)

  override def prepareRender(subWorld: SubWorld, layer: Int) {
    filledTiles.clear()
    tiles = subWorld.states(layer) map {
      case (loc, KnownState(_, _, res)) =>
        for ((typ, count) <- res)
          if (count > 0) filledTiles.getOrElseUpdate(typ, mutable.ListBuffer()) += loc
        HexTile(loc) -> Some(res)
      case (loc, GhostState(_, _, res)) =>
        for ((typ, count) <- res)
          if (count > 0) filledTiles.getOrElseUpdate(typ, mutable.ListBuffer()) += loc
        HexTile(loc) -> Some(res)
      case (loc, _) =>
        HexTile(loc) -> None
    }
  }

  override def render(g: Graphics) {
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

