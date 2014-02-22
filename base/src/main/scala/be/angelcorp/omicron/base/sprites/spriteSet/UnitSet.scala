package be.angelcorp.omicron.base.sprites.spriteSet

import com.lyndir.omicron.api.model.{IUnitType, IGameObject}
import be.angelcorp.omicron.base.sprites.Sprite
import be.angelcorp.omicron.base.bridge.Asset

trait UnitSet {

  def spriteFor( typ:   IUnitType ): Sprite
  def spriteFor( asset: Asset     ): Sprite = spriteFor( asset.gameObject.getType )

}
