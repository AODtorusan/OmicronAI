package be.angelcorp.omicron.base.gui.layerRender.renderEngine

import scala.Some
import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.base.configuration.Configuration.config
import be.angelcorp.omicron.base.gui.Canvas
import be.angelcorp.omicron.base.world._

class TerrainProvider extends SpriteProvider {

  val terrainSet = config.gui.terrainSet

  override def sprites(states: List[(Location, WorldState)], layer: Int) = {
    states.flatMap {
      case (loc, s: KnownState) =>
        val (x, y) = Canvas.center(loc)
        Some(RenderEngine.Terrain ->(terrainSet.spriteFor(loc, s.toGhost), x, y, 0f))
      case (loc, s: GhostState) =>
        val (x, y) = Canvas.center(loc)
        Some(RenderEngine.Terrain ->(terrainSet.spriteFor(loc, s), x, y, 0f))
      case _ => None
    }
  }

}
