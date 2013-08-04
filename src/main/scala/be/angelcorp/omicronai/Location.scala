package be.angelcorp.omicronai

import scala.math._
import com.lyndir.omicron.api.model._
import scala.collection.mutable.ListBuffer

/**
 * A location of a specific tile on the u-v-h game map.
 *
 * The map 2.5D is made up of 2D layers containing a sequence of hex tiles with the ''pointy side'' upwards. Several of
 * these layers are stacked along the height-axis creating the game world.
 *
 * The 2D u-v layers are wrapping, so going off the map in one directions brings you onto the map in the opposite side
 * of the map. Furthermore units can move to the six adjacent in-layer locations and straight up/down (so NOT sideways
 * up). A single layers can be visualized as follows:
 *
 * <pre>
 *  / \ / \ / \ / \ / \ / \ / \
 * |0,0|1,0|   |   |   |   |   |  --> U
 *  \ / \ / \ / \ / \ / \ / \ / \
 *   |0,1|   |   |   |   |   |   |
 *    \ / \ / \ / \ / \ / \ / \ / \
 *     |   |   |   |   |   |   |   |
 *      \ / \ / \ / \ / \ / \ / \ /
 *         \
 *          \ V
 * </pre>
 *
 * Where 0,0 = u,v is a single location object.
 *
 * @param u U-axis coordinate
 * @param v V-axis coordinate
 * @param h Height-axis coordinate
 * @param size Size of the in-plane map (u-v plane)
 */
case class Location( u: Int, v: Int, h: Int, size: Size ) {

  // Cube coordinate x
  val x = u
  // Cube coordinate y
  val y = -u-v
  // Cube coordinate z
  val z = v

  /** Construct a location based on its cube coordinates (instead of axial coordinates u|v ) */
  def this( x: Int, y: Int, z: Int, h: Int, size: Size ) = this( x, z, h, size )

  override def equals(o: Any) = o match {
    case Location( u2, v2, h2, _ ) => u == u2 && v == v2 && h == h2
    case _ => false
  }

  /** Indicates if this location is on the top layer */
  def atTop = h == 2
  /** Indicates if this location is on the bottom layer */
  def atBottom = h == 0

  /** Calculates the distance between two location in the u-axis only */
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

  /** Calculates the distance between two location in the v-axis only */
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

  /** Calculates the distance between two location in the height-axis only */
  def δh(l: Location) = l.h - h

  /** Yields the distance between height between two locations, without taking wrapping of tiles into account*/
  def δhUnwrap(l: Location) = δh(l)

  /** Calculates the distance between this location and the location defined by the offsets from this location along u/v/h.*/
  def δ(du: Int, dv: Int, dh: Int): Int = δ ( Δ (du, dv, dh) /*we do this δ(Δ) to protect against map wrapping*/ )

  /** Calculates the distance between this location and the specified location */
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

  /** Get the location given by its distance along the u, v, and h axis from this location */
  def Δ(δu: Int, δv: Int, δh: Int): Location = new Location(
    (size.getWidth  + u + δu) % size.getWidth,
    (size.getHeight + v + δv) % size.getHeight,
    h + δh, size
  )

  /** Get the location given by its direction and distance from this location */
  def Δ( direction: Direction, steps: Int = 1 ) =
    (0 until steps).foldLeft(Some(this): Option[Location])( (lastLocation, stepNr) => lastLocation match {
      case Some(loc) => loc.neighbour(direction)
      case None      => None
    } )

  /** Get the in-layer location given by its distance along the u and v axis from this location */
  def Δ2(δu: Int, δv: Int): Location = new Location(
    (size.getWidth  + u + δu) % size.getWidth,
    (size.getHeight + v + δv) % size.getHeight,
    h, size
  )

  /** Returns a list containing all the locations that neighbour this tile (including up/down if available) */
  lazy val neighbours =
    (if (atTop)    Nil else List[Location]( Δ(0, 0,  1))) :::
    (if (atBottom) Nil else List[Location]( Δ(0, 0, -1))) :::
      List[Location]( toNE, toE, toSE, toSW, toW, toNW )

  /**
   * Location laying to in a specified direction of this location.
   *
   * Note: All in-layer directions always return a tile. However when moving up and down, the map does not wrap around,
   * so no location in that direction may be present.
   */
  def neighbour( direction: Direction ): Option[Location] = direction match {
    case UP()   => if (atTop)    None else Some( Δ(0, 0,  1) )
    case DOWN() => if (atBottom) None else Some( Δ(0, 0, -1) )
    case NE()   => Some( toNE )
    case E()    => Some( toE  )
    case SE()   => Some( toSE )
    case SW()   => Some( toSW )
    case W()    => Some( toW  )
    case NW()   => Some( toNW )
  }

  /** Location laying to the North-East of this location */
  def toNE = Δ2( 1, -1)
  /** Location laying to the East of this location */
  def toE  = Δ2( 1,  0)
  /** Location laying to the South-East of this location */
  def toSE = Δ2( 0,  1)
  /** Location laying to the South-West of this location */
  def toSW = Δ2(-1,  1)
  /** Location laying to the West of this location */
  def toW  = Δ2(-1,  0)
  /** Location laying to the North-West of this location */
  def toNW = Δ2( 0, -1)

  /** List of its four mirror locations outside of the map (which is a parallelogram) */
  lazy val mirrors = List(
    new Location(u + size.getWidth, v, h, size),
    new Location(u - size.getWidth, v, h, size),
    new Location(u, v + size.getHeight, h, size),
    new Location(u, v - size.getHeight, h, size)
  )

  /**
   * Returns the right rotation (counterclockwise, 60 deg) of this tile around a specified center tile.
   *
   * For example Location(6, 5, _, _).rotateRightAround( Location(5, 5, _, _) ) yields the location Location(6, 4, _, _)
   *
   * @param l Center location for the rotations.
   */
  def rotateRightAround( l: Location ) = {
    val du = δu(l)
    val dv = δv(l)
    Location( u-dv, v+du+dv, h, size )
  }

  /**
   * Returns the left rotation (counterclockwise, 60 deg) of this tile around a specified center tile.
   *
   * For example Location(6, 5, _, _).rotateLeftAround( Location(5, 5, _, _) ) yields the location Location(5, 6, _, _)
   *
   * @param l Center location for the rotations.
   */
  def rotateLeftAround(  l: Location ) = {
    val du = δu(l)
    val dv = δv(l)
    Location( u+du+dv, v-du, h, size )
  }

  /**
   * Returns a list containing the 6 rotations (NE, E, SE, SW, W, NW) of the specified tile around (with respect to) this tile.
   *
   * For example Location(0, 0, _, _).rotationsFor( Location(0, 1, _, _) ) yields the six tiles around the tile (0, 0)
   *
   * @param l Center location for the rotations.
   */
  def rotationsFor(l: Location) = {
    val (du, dv)  = ( δu(l), δv(l) )
    val (x, y, z) = (du, -du-dv, dv)
    List( Location( u-y, v-x, h, size ), Location( u+z, v+y, h, size ), Location( u-x, v-z, h, size ),
          Location( u+y, v+x, h, size ), Location( u-z, v-y, h, size ), l )
  }

  /**
   * Gives all the hex tiles that are within the specified range from this tile.
   *
   * @param radius Maximum radius to receive locations from
   */
  def range( radius: Int ) = {
    for ( δx <- -radius to radius;
          δy <- max(-radius, -δx - radius) to min(radius, -δx + radius) ) yield
      Δ2( δx, -δx-δy )
  }

  /**
   * Returns all the tiles that make up a ring around this location at a given radius.
   *
   * @param radius Radius of the ring to create.
   */
  def ring( radius: Int ): Iterable[Location] = {
    // Move onto ring
    var last: Location = Δ( E(), radius ).get
    for( dir  <- Seq[Direction]( SW(), W(), NW(), NE(), E(), SE() );
         step <- 0 until radius ) yield {
      last = last.neighbour(dir).get
      last
    }
  }

  /**
   * Creates a spiral originating in this tile (not included in the returned list) up to a given max radius
   *
   * Note does not work in the UP-DOWN direction!
   *
   * @param maxRadius Maximum radius of the spiral.
   * @param outStep   Amount of tiles to step outwards for each spiral revolution
   */
  def spiral( maxRadius: Int, outStep: Int = 1, outDirection: Direction = E() ) = {
    require( outDirection != UP() && outDirection != DOWN()  )
    val tiles = ListBuffer[Location]()
    var H = this
    for( k <- 1 to maxRadius/outStep) {
      for (j <- 0 until outStep) {
        H = H.neighbour(outDirection).get
        tiles.append(H)
      }
      for( dir  <- Seq[Direction]( SW(), W(), NW(), NE(), E(), SE() );
           step <- 0 until k * outStep ) {
        H = H.neighbour(dir).get
        tiles.append(H)
      }
    }
    tiles.result()
  }

  /**
   * Returns all the tiles required to make a line from this location to the target end destination.
   *
   * @param end Ending location of the line.
   */
  def lineTo( end: Location ): Seq[Location] = {
    require( end.h == h )

    // Adjust one endpoint to break ties, make lines look better
    val ε = 1e-6

    val start = (x + ε, y + ε, z - 2f * ε)

    val Δx = start._1 - end.x
    val Δy = start._2 - end.y
    val Δz = start._3 - end.z
    val N = math.max(math.max(abs(Δx - Δy), abs(Δy - Δz)), abs(Δz - Δx)).ceil.toInt

    var prev: (Int, Int, Int) = null
    (for (i <- 0 to N) yield {
      val t = i.toDouble / N.toDouble
      val p = Location.hexRound(start._1 * t + end.x * (1 - t),
                                start._2 * t + end.y * (1 - t),
                                start._3 * t + end.z * (1 - t))
      if (p != prev) {
        prev = p
        Some( prev )
      } else None
    }).flatten.map( e => Location(e._1, e._3, h, size) )
  }

  def adjacentTo(l: Location) = δ(l) == 1

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
    game.getLevel(l.h).getTile(l.u, l.v).get()

  /**
   * Round hex cubic coordinates to the nearest tile
   */
  def hexRound( x: Double, y: Double, z: Double ) = {
    val rx = round(x).toInt
    val ry = round(y).toInt
    val rz = round(z).toInt

    val x_err = abs(rx - x)
    val y_err = abs(ry - y)
    val z_err = abs(rz - z)

    if (x_err > y_err && x_err > z_err)
      (-ry-rz, ry, rz)
    else if (y_err > z_err)
      (rx, -rx-rz, rz)
    else
      (rx, ry, -rx-ry)
  }

}

sealed abstract class Direction
case class UP()   extends Direction
case class DOWN() extends Direction
case class NE()   extends Direction
case class E()    extends Direction
case class SE()   extends Direction
case class SW()   extends Direction
case class W()    extends Direction
case class NW()   extends Direction
