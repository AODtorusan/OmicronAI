package be.angelcorp.omicronai.goals

import be.angelcorp.omicronai.{Location}
import be.angelcorp.omicronai.actions._
import scala.util.Random
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.agents.Squad
import akka.actor.ActorRef
import be.angelcorp.omicronai.algorithms.{MovementPathfinder, AStarSolution, AStar}
import be.angelcorp.omicronai.information.{TileInformation, TileInformationLayer, InformationLayer}

class SurveyGoal( val roi: RegionOfInterest ) extends Goal {

  def findActions[T]( units: Map[ActorRef, Option[Asset]] ) = units.mapValues( _ match {
    case Some(unit) =>
      val moveToRoi = if ( roi inArea unit.location ) Nil else {
        val solution = new MovementPathfinder( roi.center, unit ).findPath( unit.location )
        solution.path.filterNot( roi.inArea )
      }

      val roiLocation = moveToRoi.headOption match {
        case Some(head) => head
        case _ => unit.location
      }

      Seq( new MoveVia( (scoutingAStar(unit).findPath( roiLocation ).path ::: moveToRoi).reverse ) )
    case None => Nil
  } )

  def scoutingAStar(unit: Asset) = new MovementPathfinder(null, unit){
    override def heuristic(fromTile: Location) = 0
    override def goalReached(solution: AStarSolution) = solution.path.size >= 10
    override def costOnto(fromTile: Location, toTile: Location) =
      if ( roi inArea toTile) super.costOnto(fromTile, toTile) else 10 * super.costOnto(fromTile, toTile)
  }

  def informationLayers: Seq[InformationLayer] = Seq( new TileInformationLayer{
    val name  = "Region of interest"
    val tiles = roi.tiles.map( new TileInformation( _, 0.5f) )
  } )

}



