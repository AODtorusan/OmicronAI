package be.angelcorp.omicron.base.gui.layerRender.renderEngine

import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.base.world.WorldState
import be.angelcorp.omicron.base.sprites.Sprite

trait SpriteProvider {
  type SpriteLayer = Int

  def sprites( states: List[(Location, WorldState)], layer: Int ): Iterable[(SpriteLayer, (Sprite, Float, Float, Float))]

}
