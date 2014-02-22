package be.angelcorp.omicron.base.sprites.spriteSet

import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.base.sprites.Sprite
import be.angelcorp.omicron.base.world.GhostState

trait TerrainSet {

  def spriteFor(l: Location, state: GhostState): Sprite
  
}
