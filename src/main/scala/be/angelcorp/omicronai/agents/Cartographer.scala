package be.angelcorp.omicronai.agents

import scala.collection.mutable
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import com.lyndir.omicron.api.controller.GameController
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.ResourceType._
import be.angelcorp.omicronai.Location


class Cartographer(val gameController: GameController) extends Agent {
  case class TacticalInfo( value: Double, confidence: Double )
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit val game = gameController.getGame
  val name = "Cartographer"

  private val fuel         = mutable.Map[Location, TacticalInfo]()
  private val silicon      = mutable.Map[Location, TacticalInfo]()
  private val metal        = mutable.Map[Location, TacticalInfo]()
  private val rare_element = mutable.Map[Location, TacticalInfo]()

  def act = {
    case UpdateLocation( location ) =>
      logger.trace(s"Updating map information on location $location")
      val tile: Tile = location
      updateResources(location, tile)

    case ResourcesOn(location: Location, typ: ResourceType ) =>
      sender ! resourcesOn( location, typ )

    case ThreatOn( location: Location ) =>
      sender ! ThreatIndex( location, 0.0, 0.0 )

    case seq: Seq[_] =>
      sender ! seq.map( e => e match {
        case ResourcesOn(location: Location, typ: ResourceType ) =>
          Some(resourcesOn( location, typ ))
        case _ => None
      } ).flatten

    case SubmitActions() => sender ! Ready()
  }

  def resourcesOn(location: Location, typ: ResourceType) = {
    val entry = (typ match {
      case FUEL          => fuel
      case SILICON       => silicon
      case METALS        => metal
      case RARE_ELEMENTS => rare_element
    }).getOrElse( location, TacticalInfo(0.0, 0.0) )
    ResourceCount( location, typ, entry.value, entry.confidence )
  }

  def updateResources(location: Location, tile: Tile) = {
    fuel         update(location, TacticalInfo(tile.getResourceQuantity(FUEL), 1.0) )
    silicon      update(location, TacticalInfo(tile.getResourceQuantity(SILICON), 1.0) )
    metal        update(location, TacticalInfo(tile.getResourceQuantity(METALS), 1.0) )
    rare_element update(location, TacticalInfo(tile.getResourceQuantity(RARE_ELEMENTS), 1.0) )
  }

}

case class UpdateLocation( l: Location )

case class ResourcesOn( l: Location, typ: ResourceType )
case class ResourceCount( l: Location, typ: ResourceType, quantity: Double, confidence: Double )

case class ThreatOn( l: Location )
case class ThreatIndex( l: Location, threat: Double, confidence: Double )
