package be.angelcorp.omicronai.algorithms

import java.util
import java.util.Comparator
import collection.JavaConverters._
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.{Direction, Location}
import be.angelcorp.omicronai.metadata.{PathfinderMetadata, MetaData}
import be.angelcorp.omicronai.world.WorldGraph

abstract class AStar {

  def heuristic( fromTile: Location): Double

  def costGraph: WorldGraph[_, Double]

  def goalReached( solution: AStarSolution ): Boolean

  def findPath( origin: Location): (AStarSolution, MetaData) = {

    val open   = new util.HashMap[Location, AStarSolution]()
    val closed = new util.HashMap[Location, AStarSolution]()

    val originSolution = new AStarSolution(0, heuristic(origin), List(origin))
    open.put( origin, originSolution )

    if ( goalReached( originSolution ) ) {
      return (originSolution, new PathfinderMetadata(originSolution, Nil))
    }

    while ( !open.isEmpty ) {
      val q = open.values().iterator().asScala.minBy( _.f )

      open.remove( q.tile )
      closed.put( q.tile, q )

      for ( (direction, target) <- q.tile.neighbours; cost <- costGraph.edgeAt(q.tile, direction) ) {
        val g = q.g + cost
        val h = heuristic(target)
        val targetSubSolution = new AStarSolution(g, h, target :: q.path)

        if ( goalReached(targetSubSolution) )
          return (targetSubSolution, new PathfinderMetadata(targetSubSolution, closed.values().asScala.toSeq ++ open.values().asScala.toSeq ))
        else if ( (!closed.containsKey( target ) || targetSubSolution.f < closed.get( target ).f ) &&
                  (!open.containsKey  ( target ) || targetSubSolution.f < open.get(target).f     ) )
          open.put( target, targetSubSolution )
      }
    }
    throw new IllegalArgumentException("AStar did not find any solution")
  }
}

object AStar{
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def apply( destination: Location ) = new AStar {
    def costGraph = new WorldGraph[Null, Double] {
      def tileAt( l: Location ) = ???
      def edgeAt( l: Location, d: Direction ) = Some(1.0)
    }
    def heuristic(fromTile: Location) = 2.0 * math.abs(fromTile δ destination)
    def goalReached(solution: AStarSolution) = destination == solution.tile
  }

}

class AStarSolution( val g: Double, val h: Double, val path: List[ Location ] ) {

  val f    = g + h
  val tile = path.head

  override lazy val toString = s"$tile | f=$f, g=$g, h=$h"
}

