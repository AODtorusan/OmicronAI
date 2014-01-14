package be.angelcorp.omicronai

import scala.math._
import com.lyndir.omicron.api.model.{Tile, Coordinate}


case class HexTile(u: Int, v: Int) {

  // Construct from cubic coordinates
  def this( x: Int, y: Int, z: Int ) = this( x, z )

  def +( du: Int, dv: Int ) = HexTile(u+du, v+dv)

  val centerXY = ( (u + (v - v%1) / 2f) * HexTile.width, v * 0.75f * HexTile.height )

  val verticesXY = Seq(
    ( 0.00f * HexTile.width + centerXY._1,  0.50f * HexTile.height + centerXY._2),
    ( 0.50f * HexTile.width + centerXY._1,  0.25f * HexTile.height + centerXY._2),
    ( 0.50f * HexTile.width + centerXY._1, -0.25f * HexTile.height + centerXY._2),
    ( 0.00f * HexTile.width + centerXY._1, -0.50f * HexTile.height + centerXY._2),
    (-0.50f * HexTile.width + centerXY._1, -0.25f * HexTile.height + centerXY._2),
    (-0.50f * HexTile.width + centerXY._1,  0.25f * HexTile.height + centerXY._2)
  )

  val edgesXY = List[HexTileEdge](
    HexTileNE(this), HexTileE(this), HexTileSE(this), HexTileSW(this), HexTileW(this), HexTileNW(this)
  )

}

sealed abstract class HexTileEdge() {
  val startXY: (Float, Float)
  val endXY:   (Float, Float)
}
case class HexTileNE(tile: HexTile) extends HexTileEdge {
  val startXY = ( 0.00f * HexTile.width + tile.centerXY._1,  0.50f * HexTile.height + tile.centerXY._2)
  val endXY   = ( 0.50f * HexTile.width + tile.centerXY._1,  0.25f * HexTile.height + tile.centerXY._2)
  override def equals(obj: Any) = obj match {
    case Some(edge: HexTileNE) => edge.tile == tile
    case Some(edge: HexTileSW) => edge.tile == HexTile(-1, 1)
    case _ => false
  }
}
case class HexTileE(tile: HexTile) extends HexTileEdge {
  val startXY = ( 0.50f * HexTile.width + tile.centerXY._1,  0.25f * HexTile.height + tile.centerXY._2)
  val endXY   = ( 0.50f * HexTile.width + tile.centerXY._1, -0.25f * HexTile.height + tile.centerXY._2)
  override def equals(obj: Any) = obj match {
    case Some(edge: HexTileE) => edge.tile == tile
    case Some(edge: HexTileW) => edge.tile == tile + (-1, 0)
    case _ => false
  }
}
case class HexTileSE(tile: HexTile) extends HexTileEdge {
  val startXY = ( 0.50f * HexTile.width + tile.centerXY._1, -0.25f * HexTile.height + tile.centerXY._2)
  val endXY   = ( 0.00f * HexTile.width + tile.centerXY._1, -0.50f * HexTile.height + tile.centerXY._2)
  override def equals(obj: Any) = obj match {
    case Some(edge: HexTileSE) => edge.tile == tile
    case Some(edge: HexTileNW) => edge.tile == tile + (0, -1)
    case _ => false
  }
}
case class HexTileSW(tile: HexTile) extends HexTileEdge {
  val startXY = ( 0.00f * HexTile.width + tile.centerXY._1, -0.50f * HexTile.height + tile.centerXY._2)
  val endXY   = (-0.50f * HexTile.width + tile.centerXY._1, -0.25f * HexTile.height + tile.centerXY._2)
  override def equals(obj: Any) = obj match {
    case Some(edge: HexTileSW) => edge.tile == tile
    case Some(edge: HexTileNE) => edge.tile == tile + ( 1, -1)
    case _ => false
  }
}
case class HexTileW(tile: HexTile) extends HexTileEdge {
  val startXY = (-0.50f * HexTile.width + tile.centerXY._1, -0.25f * HexTile.height + tile.centerXY._2)
  val endXY   = (-0.50f * HexTile.width + tile.centerXY._1,  0.25f * HexTile.height + tile.centerXY._2)
  override def equals(obj: Any) = obj match {
    case Some(edge: HexTileW) => edge.tile == tile
    case Some(edge: HexTileE) => edge.tile == tile + (1, 0)
    case _ => false
  }
}
case class HexTileNW(tile: HexTile) extends HexTileEdge {
  val startXY = (-0.50f * HexTile.width + tile.centerXY._1,  0.25f * HexTile.height + tile.centerXY._2)
  val endXY   = ( 0.00f * HexTile.width + tile.centerXY._1,  0.50f * HexTile.height + tile.centerXY._2)
  override def equals(obj: Any) = obj match {
    case Some(edge: HexTileNW) => edge.tile == tile
    case Some(edge: HexTileSE) => edge.tile == tile + (0, 1)
    case _ => false
  }
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
