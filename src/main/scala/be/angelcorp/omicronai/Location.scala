package be.angelcorp.omicronai

import scala.math._
import com.lyndir.omnicron.api.model._

case class Location( u: Int, v: Int, h: Int, size: Size ) {

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

  lazy val neighbours =
    (if (atTop)    Nil else List[Location]( Δ(0, 0,  1))) :::
    (if (atBottom) Nil else List[Location]( Δ(0, 0, -1))) :::
    List[Location]( Δ2( 0, -1), Δ2( 1, -1), Δ2(-1,  0), Δ2( 1,  0), Δ2(-1,  1), Δ2( 0,  1) )

  def atTop =    h == 2
  def atBottom = h == 0

  def δu(l: Location) = {
    val du = u - l.u

    if (du > size.getWidth / 2)
      du - size.getWidth
    else if (du < -size.getWidth / 2)
      du + size.getWidth
    else
      du
  }
  def δv(l: Location) = {
    val dv = v - l.v

    if (dv > size.getHeight / 2)
      dv - size.getHeight
    else if (dv < -size.getHeight / 2)
      dv + size.getHeight
    else
      dv
  }

  def δh(l: Location) = h - l.h

  def δ(du: Int, dv: Int, dh: Int): Int = (abs(du) + abs(dv) + abs(du + dv)) / 2 + dh

  def δ(l: Location): Int =  δ( δu(l), δv(l), δh(l) )

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

}

object Location {

  implicit def level2int(level: Level) = level match {
    case ground: GroundLevel => 0
    case sky:    SkyLevel    => 1
    case space:  SpaceLevel  => 2
  }
  implicit def int2level(level: Int): Level = throw new UnsupportedOperationException

  implicit def tile2location( tile: Tile ) =
    new Location( tile.getPosition.getU, tile.getPosition.getV, tile.getLevel, tile.getLevel.getLevelSize )
  implicit def location2tile( l: Location ) =
    new Tile( new Coordinate(l.u, l.v, l.size), l.h )

}
