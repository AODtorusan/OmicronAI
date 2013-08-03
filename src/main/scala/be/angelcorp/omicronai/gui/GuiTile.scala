package be.angelcorp.omicronai.gui

import org.newdawn.slick.{Color, Graphics}
import scala.math._
import org.newdawn.slick.geom.{Vector2f, Polygon}
import be.angelcorp.omicronai.{HexTile, Location}

class GuiTile( val location: HexTile ) {

  val openGLvertices = location.verticesXY.map( v => Seq(v._1 * GuiTile.scale, v._2 * GuiTile.scale) ).flatten.toArray

  def borderStyle: DrawStyle = Color.white
  def fillColor:   Color     = Color.transparent
  def textColor:   Color     = Color.transparent

  def text: String = ""

  def render(g: Graphics) {
    val shape = new Polygon( openGLvertices )
    val center = new Vector2f(shape.getCenter)
    if (fillColor != Color.transparent) {
      g.setColor( fillColor )
      g.fill(shape)
    }
    if (borderStyle.color != Color.transparent) {
      borderStyle.applyOnto(g)
      g.draw(shape)
    }

    val h = g.getFont.getHeight(text)
    val w = g.getFont.getWidth(text)
    g.setColor(textColor)
    g.drawString( text, center.getX - w / 2.0f, center.getY - 2.0f * h / 3.0f )
  }

}

object GuiTile {
  val scale  = 100f

  def center(l: Location): (Float, Float) = center( l.u, l.v )

  def center(u: Int, v: Int): (Float, Float) = {
    val unscaled = HexTile(u, v).centerXY
    ( unscaled._1 * scale, unscaled._2 * scale )
  }

}
