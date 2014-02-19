package be.angelcorp.omicron.base.gui

import org.newdawn.slick.{Color, Graphics}
import org.newdawn.slick.geom.Polygon
import be.angelcorp.omicron.base.{Location, HexTile}
import be.angelcorp.omicron.base.gui.slick.DrawStyle

class Canvas( val location: HexTile ) {

  def borderStyle: DrawStyle = Color.white
  def fillColor:   Color     = Color.transparent
  def textColor:   Color     = Color.transparent

  def text: String = ""

  def render(g: Graphics) {
    Canvas.render(g, location, borderStyle, fillColor)

    val msg = text
    if (!msg.isEmpty)
      Canvas.text(g, location, msg, textColor)
  }

}

object Canvas {
  val scale  = 64f

  def center(l: Location): (Float, Float) = center( l.u, l.v )

  def center(tile: HexTile): (Float, Float) = {
    val unscaled = tile.centerXY
    ( unscaled._1 * scale, unscaled._2 * scale )
  }

  def center(u: Int, v: Int): (Float, Float) = center( HexTile(u, v) )

  val openGLvertices = HexTile(0,0).verticesXY.map( v => Seq(v._1 * Canvas.scale, v._2 * Canvas.scale) ).flatten.toArray

  def render(g: Graphics, tile: HexTile, border: DrawStyle, fill: DrawStyle) {
    render(g, Seq(tile), border, fill)
  }

  def render(g: Graphics, tiles: Iterable[HexTile], border: DrawStyle, fill: DrawStyle = Color.transparent) {
    val openGLshapes = tiles.map( t => {
      val tileCenter = t.centerXY
      val movedOpenGLvertices = Array.ofDim[Float]( 12 )
      for (i <- 0 until openGLvertices.size by 2) {
        movedOpenGLvertices( i ) = openGLvertices( i ) + tileCenter._1 * scale
        movedOpenGLvertices(i+1) = openGLvertices(i+1) + tileCenter._2 * scale
      }
      new Polygon( movedOpenGLvertices )
    })

    if (fill.color != Color.transparent) {
      fill.applyOnto(g)
      openGLshapes.foreach( g.fill )
    }
    if (border.color != Color.transparent) {
      border.applyOnto(g)
      openGLshapes.foreach( g.draw )
    }
  }

  def text(g: Graphics, t: HexTile, text: String, color: Color = Color.white) {
    val h = g.getFont.getHeight(text)
    val w = g.getFont.getWidth(text)
    val c = t.centerXY
    g.setColor(color)
    g.drawString( text, c._1 * scale - w / 2.0f, c._2 * scale - 2.0f * h / 3.0f )
  }

  def line(g: Graphics, from: HexTile, to: HexTile, lineStyle: DrawStyle ) {
    lineStyle.applyOnto(g)
    val (fromX, fromY) = center(from)
    val (toX,   toY  ) = center(to)
    g.drawLine(fromX, fromY, toX, toY)
  }

}
