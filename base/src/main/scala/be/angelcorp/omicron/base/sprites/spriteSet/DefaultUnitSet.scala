package be.angelcorp.omicron.base.sprites.spriteSet

import org.newdawn.slick.{Color, Image}
import com.lyndir.omicron.api.model.{UnitTypes, IUnitType}
import be.angelcorp.omicron.base.gui.Canvas
import be.angelcorp.omicron.base.sprites.{Sprites, StaticSprite, Sprite}
import scala.util.Success

class DefaultUnitSet extends UnitSet {

  val unitSprites = {
    for ( typ <- UnitTypes.values() ) yield
      (typ: IUnitType) -> Sprites.findSprite("defaultUnits." + typ.toString.toLowerCase).getOrElse( buildSprite(typ) )
  }.toMap

  def buildSprite(unitType: IUnitType): Sprite = {
    val sz   = Canvas.scale.toInt * 2
    val img  = new Image(sz, sz)
    val g    = img.getGraphics
    val font = g.getFont
    val name = unitType.toString
    font.drawString(sz / 2 - (font.getWidth(name) / 2), sz/2 - font.getLineHeight/2, name, Color.black)
    new StaticSprite( unitType.toString, img.getScaledCopy(0.5f) )
  }

  override def spriteFor(typ: IUnitType): Sprite = unitSprites.get( typ ).get

}
