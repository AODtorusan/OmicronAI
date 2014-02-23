package be.angelcorp.omicron.base.sprites.spriteSet

import com.lyndir.omicron.api.model.{IUnitType, IGameObject}
import be.angelcorp.omicron.base.sprites.Sprite
import be.angelcorp.omicron.base.bridge.Asset

trait UnitSet {

  def spriteFor( typ:   IUnitType ): UnitGraphics
  def spriteFor( asset: Asset     ): UnitGraphics = spriteFor( asset.gameObject.getType )

}

case class UnitGraphics(unit: Sprite, shadow: Option[Sprite] = None) {
  def this(unit: Sprite, shadow: Sprite) = this( unit, Some(shadow) )
}