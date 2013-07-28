package be.angelcorp.omicronai

import scala.math._
import com.lyndir.omicron.api.model._

case class Location( val u: Int, val v: Int, val h: Int, val size: Size ) {

  // Cube coordinate x
  val x = u
  // Cube coordinate y
  val y = -u-v
  // Cube coordinate z
  val z = v

  def -( l2: Location ) = ( u - l2.u, v - l2.v, h - l2.h )

  override def equals(o: Any) = o match {
    case Location( u2, v2, h2, _ ) => u == u2 && v == v2 && h == h2
    case _ => false
  }

  def atTop =    h == 2
  def atBottom = h == 0

  def δu(l: Location) = {
    val du = l.u - u

    if (du > size.getWidth / 2)
      du - size.getWidth
    else if (du < -size.getWidth / 2)
      du + size.getWidth
    else
      du
  }

  /** Yields the distance between in the u-axis between two locations, without taking wrapping of tiles into account*/
  def δuUnwrap(l: Location) = l.u - u

  def δv(l: Location) = {
    val dv = l.v - v

    if (dv > size.getHeight / 2)
      dv - size.getHeight
    else if (dv < -size.getHeight / 2)
      dv + size.getHeight
    else
      dv
  }

  /** Yields the distance between in the v-axis between two locations, without taking wrapping of tiles into account*/
  def δvUnwrap(l: Location) = l.v - v

  def δh(l: Location) = l.h - h

  /** Yields the distance between height between two locations, without taking wrapping of tiles into account*/
  def δhUnwrap(l: Location) = δh(l)

  def δ(du: Int, dv: Int, dh: Int): Int = δ ( Δ (du, dv, dh) )

  def δ(l: Location): Int = {
    val du = δu(l)
    val dv = δv(l)
    (abs(du) + abs(dv) + abs(du + dv)) / 2 + δh(l)
  }

  /** Yields the distance between two locations, without taking wrapping of tiles into account*/
  def δunwrap(l: Location): Int = {
    val du = δuUnwrap(l)
    val dv = δvUnwrap(l)
    (abs(du) + abs(dv) + abs(du + dv)) / 2 + δhUnwrap(l)
  }

  def Δ(l: Location): Location = Δ( δu(l), δv(l), δh(l) )

  def Δ(δu: Int, δv: Int, δh: Int): Location = new Location(
    (size.getWidth  + u + δu) % size.getWidth,
    (size.getHeight + v + δv) % size.getHeight,
    h + δh, size
  )

  def Δ2(l: Location): Location = Δ2( δu(l), δv(l) )

  def Δ2(δu: Int, δv: Int): Location = new Location(
    (size.getWidth  + u + δu) % size.getWidth,
    (size.getHeight + v + δv) % size.getHeight,
    h, size
  )

  lazy val neighbours =
    (if (atTop)    Nil else List[Location]( Δ(0, 0,  1))) :::
      (if (atBottom) Nil else List[Location]( Δ(0, 0, -1))) :::
      List[Location]( Δ2( 0, -1), Δ2( 1, -1), Δ2(-1,  0), Δ2( 1,  0), Δ2(-1,  1), Δ2( 0,  1) )

  lazy val mirrors = List(
    new Location(u + size.getWidth, v, h, size),
    new Location(u - size.getWidth, v, h, size),
    new Location(u, v + size.getHeight, h, size),
    new Location(u, v - size.getHeight, h, size)
  )

  def adjacentTo(l: Location) = neighbours.contains( l )

  override def toString: String = s"Location($u, $v, $h)"

}

object Location {

  implicit def levelType2int(level: LevelType): Int = level.ordinal()
  implicit def int2levelType(level: Int): LevelType = LevelType.values()(level)

  implicit def level2int(level: Level): Int = level.getType
  implicit def int2level(level: Int)(implicit game: Game): Level = game.getLevel( level )

  implicit def tile2location( tile: Tile ) =
    new Location( tile.getPosition.getU, tile.getPosition.getV, tile.getLevel, tile.getLevel.getSize )

  implicit def location2tile( l: Location )(implicit game: Game) =
    new Tile( new Coordinate(l.u, l.v, l.size), l.h )

}
