package be.angelcorp.omicronai.goals

import be.angelcorp.omicronai.{AStarSolution, Location, AStar}
import be.angelcorp.omicronai.actions._
import scala.util.Random
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.agents.Squad
import akka.actor.ActorRef

class SurveyGoal( val roi: RegionOfInterest ) extends Goal {

  def findActions[T]( units: Map[ActorRef, Option[Asset]] ) = units.mapValues( _ match {
    case Some(unit) =>
      val moveToRoi = if ( roi inArea unit.location ) Nil else {
        AStar(roi.center).findPath( unit.location ).path.filterNot( roi.inArea ).map( new MoveTo( _ ) )
      }

      val roiLocation = if ( moveToRoi.isEmpty ) unit.location else moveToRoi.head.destination

      (scoutingAStar.findPath( roiLocation ).path.map( new MoveTo( _ ) ) ::: moveToRoi).reverse
    case None => Nil
  } )

  def scoutingAStar = new AStar{
    override def heuristic( fromTile: Location ) = 0
    override def costOnto(  fromTile: Location, toTile: Location ) = {
      val terrainCost = 1
      val benefits = 1
      terrainCost - benefits
    }
    def goalReached(solution: AStarSolution) = solution.path.size >= 10
  }

}



