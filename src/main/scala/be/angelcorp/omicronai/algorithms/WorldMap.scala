package be.angelcorp.omicronai.algorithms

import be.angelcorp.omicronai._
import scala.reflect.ClassTag

class WorldMap[Tile, Edge]( val tiles:   Field[Tile],
                            val up_down: Field[Edge],
                            val ne_sw:   Field[Edge],
                            val e_w:     Field[Edge],
                            val se_nw:   Field[Edge]) {

  def tileAt( l: Location ) = tiles(l)

  def edgeAt( l: Location, d: Direction ) = d match {
    case UP()   => if (l.atTop)    None else Some(up_down(l))
    case DOWN() => if (l.atBottom) None else Some(up_down(l Δ (0,0,-1)))
    case NE()   => Some( ne_sw(l) )
    case E()    => Some( e_w  (l) )
    case SE()   => Some( se_nw(l) )
    case SW()   => Some( ne_sw(l.toSW) )
    case W()    => Some( e_w  (l.toW ) )
    case NW()   => Some( se_nw(l.toNW) )
  }

  def map[T: ClassTag, E: ClassTag]( tileTransform: Tile => T, edgeTransform: Edge => E ) =
    new WorldMap[T, E]( tiles.map(tileTransform), up_down.map(edgeTransform), ne_sw.map(edgeTransform), e_w.map(edgeTransform), se_nw.map(edgeTransform) )

  def mapEdges[E: ClassTag]( transform: Edge => E ) =
    new WorldMap[Tile, E]( tiles, up_down.map(transform), ne_sw.map(transform), e_w.map(transform), se_nw.map(transform) )

  def mapTiles[T: ClassTag]( transform: Tile => T ) =
    new WorldMap[T, Edge]( tiles.map(transform), up_down, ne_sw, e_w, se_nw )

}