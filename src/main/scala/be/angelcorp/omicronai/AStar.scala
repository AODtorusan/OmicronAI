package be.angelcorp.omicronai

import java.util
import java.util.Comparator
import collection.JavaConverters._

abstract class AStar {

  def heuristic( fromTile: Location): Int

  def costOnto( fromTile: Location, toTile: Location ): Int

  def goalReached( solution: AStarSolution ): Boolean

  def findPath( origin: Location): AStarSolution = {

    val open   = new util.TreeSet[AStarSolution]( new Comparator[AStarSolution]{
      def compare(o1: AStarSolution, o2: AStarSolution) = o1.f.compareTo(o2.f)
    })
    val closed = new util.TreeSet[AStarSolution]( new Comparator[AStarSolution]{
      def compare(o1: AStarSolution, o2: AStarSolution) = o1.f.compareTo(o2.f)
    })

    open.add( new AStarSolution(0, heuristic(origin), List(origin)) )

    while ( !open.isEmpty ) {
      val q = open.first()
      q.tile.neighbours.map( target => {
        val g = q.g + costOnto( q.tile, target )
        val h = heuristic(target)
        val targetSubSolution = new AStarSolution(g, h, target :: q.path)

        if ( goalReached(targetSubSolution) )
          return targetSubSolution
        else {
          open.iterator().asScala.find( ss => ss.tile == target ) match {
            case Some(identical) if targetSubSolution.f < identical.f =>
            case Some(identical) =>
              open.remove( identical )
              open.add( targetSubSolution )
            case _ =>
              closed.iterator().asScala.find( ss => ss.tile == target ) match {
                case Some(identical) if targetSubSolution.f > identical.f =>
                case Some(identical) =>
                  closed.remove( identical )
                  open.add( targetSubSolution )
                case _ =>
                  open.add( targetSubSolution )
              }
          }
        }
      } )
      closed.add( q )
    }
    throw new IllegalArgumentException("AStar did not find any solution")
  }
}

object AStar{

  def apply( destination: Location ) = new AStar {
    def heuristic(fromTile: Location): Int = fromTile δ destination
    def costOnto(fromTile: Location, toTile: Location) = 1
    def goalReached(solution: AStarSolution) = destination == solution.tile
  }

}

class AStarSolution( val g: Int, val h: Int, val path: List[ Location ] ) {

  val f    = g + h
  val tile = path.head

}

