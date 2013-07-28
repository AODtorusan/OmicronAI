package be.angelcorp.omicronai.goals

import scala.concurrent._
import scala.concurrent.duration._
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import be.angelcorp.omicronai.{Location}
import be.angelcorp.omicronai.actions._
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.algorithms.{MovementPathfinder, AStarSolution}
import be.angelcorp.omicronai.agents.{Soldier, Self}

class SurveyGoal( val roi: RegionOfInterest ) extends Goal {

  def findActions[T]( units: Map[ActorRef, Option[Asset]] ) = units.map( e => e._2 match {
    case Some(unit) =>
      val moveToRoi = if ( roi inArea unit.location ) Nil else {
        val solution = new MovementPathfinder( roi.center, unit ).findPath( unit.location )
        solution._1.path.filterNot( roi.inArea )
      }

      val roiLocation = moveToRoi.headOption match {
        case Some(head) => head
        case _ => unit.location
      }

      implicit val timeout: Timeout = 5 seconds;
      val soldier = Await.result( ask(e._1, Self()), timeout.duration).asInstanceOf[Soldier]
      (e._1, Seq( new MoveVia( (scoutingAStar(unit).findPath( roiLocation )._1.path ::: moveToRoi).reverse, soldier ) ))
    case None => (e._1, Nil)
  } )

  def scoutingAStar(unit: Asset) = new MovementPathfinder(null, unit){
    override def heuristic(fromTile: Location) = 0
    override def goalReached(solution: AStarSolution) = solution.path.size >= 10
    override def costOnto(fromTile: Location, toTile: Location) =
      if ( roi inArea toTile) super.costOnto(fromTile, toTile) else 10 * super.costOnto(fromTile, toTile)
  }

}



