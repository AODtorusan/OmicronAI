package be.angelcorp.omicronai

import scala.math._
import com.lyndir.omicron.api.model.{Tile, Coordinate}

case class HexTile(u: Int, v: Int) {

  val centerXY = ( (u + (v - v%1) / 2f) * HexTile.width, v * 0.75f * HexTile.height )

  val verticesXY = Seq(
    ( 0.00f * HexTile.width + centerXY._1,  0.50f * HexTile.height + centerXY._2),
    ( 0.50f * HexTile.width + centerXY._1,  0.25f * HexTile.height + centerXY._2),
    ( 0.50f * HexTile.width + centerXY._1, -0.25f * HexTile.height + centerXY._2),
    ( 0.00f * HexTile.width + centerXY._1, -0.50f * HexTile.height + centerXY._2),
    (-0.50f * HexTile.width + centerXY._1, -0.25f * HexTile.height + centerXY._2),
    (-0.50f * HexTile.width + centerXY._1,  0.25f * HexTile.height + centerXY._2)
  )

}

object HexTile {

  val height = 1.0f
  val width  = (sqrt(3.0)/2.0 * height).toFloat
  val radius = height / 2f

  def fromXY( _x: Float, _y: Float) = {
    val x = _x / width
    val y = _y / width

    val temp = floor(x + sqrt(3) * y + 1)
    val r = floor((temp + floor(-x + sqrt(3) * y + 1))/3).toInt
    val q = floor((floor(2*x+1) + temp) / 3).toInt - r
    HexTile(q, r)
  }

  def apply(l: Location): HexTile = HexTile(l.u, l.v)

  implicit def location2hexTile(l: Location) =
    new HexTile(l.u, l.v)

  implicit def coordinate2hexTile(c: Coordinate) =
    new HexTile(c.getU, c.getV)

  implicit def tile2hexTile(t: Tile) =
    coordinate2hexTile( t.getPosition )

}
