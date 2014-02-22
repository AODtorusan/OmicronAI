package be.angelcorp.omicron.base.sprites.spriteSet

import scala.util.Random
import scala.collection.mutable
import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.base.world.GhostState
import be.angelcorp.omicron.base.sprites.{Sprites, Sprite}

class DefaultTerrainSet extends TerrainSet {

  val mapCache = mutable.Map[Location, Sprite]()

  val terrainTypes = Map(
    Sprites.findSprite("defaultTerrain.grass").get         -> 0.8,
    Sprites.findSprite("defaultTerrain.frozen_ground").get -> 0.1,
    Sprites.findSprite("defaultTerrain.moss").get          -> 0.1
  )

  override def spriteFor(l: Location, state: GhostState): Sprite = {
    mapCache.getOrElseUpdate( l,
      randomTerrain(state)
    )
  }

  def randomTerrain( state: GhostState ) = {
    terrainTypes.find( entry => entry._2 > Random.nextDouble()) match {
      case Some((sprite, _)) => sprite
      case _ => terrainTypes.keys.head
    }
  }

}
