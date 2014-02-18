package be.angelcorp.omicronai.world

import scala.reflect.ClassTag
import be.angelcorp.omicronai._
import be.angelcorp.omicronai.algorithms.Field

trait WorldGraph[Tile, Edge] {

  def tileAt( l: Location ): Tile
  def edgeAt( l: Location, d: Direction ): Option[Edge]

}

class FieldWorldGraph[Tile, Edge](val tiles:   Field[Tile],
                                  val up_down: Field[Edge],
                                  val ne_sw:   Field[Edge],
                                  val e_w:     Field[Edge],
                                  val se_nw:   Field[Edge]) extends WorldGraph[Tile, Edge] {

  def tileAt( l: Location ) = tiles(l)

  def edgeAt( l: Location, d: Direction ) = d match {
    case UP()   => if (l.atTop)    None else Some(up_down(l))
    case DOWN() => if (l.atBottom) None else (l Δ (0,0,-1)).map( l2 => up_down(l2))
    case NE()   => Some( ne_sw(l) )
    case E()    => Some( e_w  (l) )
    case SE()   => Some( se_nw(l) )
    case SW()   => l.toSW map( l2 => ne_sw(l2) )
    case W()    => l.toW  map( l2 => e_w( l2 ) )
    case NW()   => l.toNW map( l2 => se_nw(l2) )
  }

  def map[T: ClassTag, E: ClassTag]( tileTransform: Tile => T, edgeTransform: Edge => E ) =
    new FieldWorldGraph[T, E]( tiles.map(tileTransform), up_down.map(edgeTransform), ne_sw.map(edgeTransform), e_w.map(edgeTransform), se_nw.map(edgeTransform) )

  def mapEdges[E: ClassTag]( transform: Edge => E ) =
    new FieldWorldGraph[Tile, E]( tiles, up_down.map(transform), ne_sw.map(transform), e_w.map(transform), se_nw.map(transform) )

  def mapTiles[T: ClassTag]( transform: Tile => T ) =
    new FieldWorldGraph[T, Edge]( tiles.map(transform), up_down, ne_sw, e_w, se_nw )

}