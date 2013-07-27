package be.angelcorp.omicronai.gui

import org.newdawn.slick.{Color, Graphics}
import scala.math._
import org.newdawn.slick.geom.{Vector2f, Polygon}
import be.angelcorp.omicronai.Location

class GuiTile( val location: Location ) {

  val center   = GuiTile.unscaledCenter( location )
  val vertices = GuiTile.vertices( center ).map( v => Seq(v._1, v._2) ).flatten.toArray

  def borderStyle: DrawStyle = new DrawStyle(Color.white)
  def fillColor:   Color = Color.black
  def textColor:   Color = Color.white

  def text: String = ""

  def render(g: Graphics) {
    val shape = new Polygon( vertices )
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
  val height = 1.0f
  val width  = (sqrt(3.0)/2.0 * height).toFloat

  val unscaledVertices = Seq( ( 0.00f,  0.50f),
                              ( 0.50f,  0.25f),
                              ( 0.50f, -0.25f),
                              ( 0.00f, -0.50f),
                              (-0.50f, -0.25f),
                              (-0.50f,  0.25f))

  def vertices(loc: Location): Seq[(Float, Float)] =
    vertices( loc.u, loc.v )

  def vertices(u: Int, v: Int): Seq[(Float, Float)] =
    vertices( unscaledCenter(u, v) )

  def vertices(center: (Float, Float)): Seq[(Float, Float)] = unscaledVertices.map( v => {
    ( GuiTile.scale * (v._1 * GuiTile.width  + center._1),
      GuiTile.scale * (v._2 * GuiTile.height + center._2))
  } )

  def unscaledCenter(location: Location): (Float, Float) =
    unscaledCenter(location.u, location.v)

  def unscaledCenter(u: Int, v: Int): (Float, Float) =
    ( (u + (v - v%1) / 2.0f) * width, v * 0.75f * height )

  def center(location: Location): (Float, Float) = {
    val c = unscaledCenter(location)
    (c._1 * scale, c._2 * scale)
  }

  def center(u: Int, v: Int): (Float, Float) = {
    val c = unscaledCenter(u, v)
    (c._1 * scale, c._2 * scale)
  }

}
