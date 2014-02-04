package be.angelcorp.omicronai.ai.pike.agents.squad

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.{ActorRef, Props}
import org.slf4j.LoggerFactory
import org.newdawn.slick.Color
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.{ResourceType, Tile, Player}
import be.angelcorp.omicronai.algorithms.MovementPathfinder
import be.angelcorp.omicronai.Conversions._
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.configuration.Configuration
import Configuration.config
import be.angelcorp.omicronai.Location.location2tile
import be.angelcorp.omicronai.{Location, RegionOfInterest, Namer}
import be.angelcorp.omicronai.gui.layerRender.{PolyLineRenderer, LayerRenderer, RegionRenderer}
import be.angelcorp.omicronai.metadata.MetaData
import be.angelcorp.omicronai.ai.pike.agents._
import be.angelcorp.omicronai.gui.slick.DrawStyle

class SurveySquad(val owner: Player,
                  val name: String,
                  val cartographer: ActorRef ) extends Squad {
  import context.dispatcher
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit def timeout: Timeout = config.ai.messageTimeout seconds;
  logger.debug(s"Created a new survey squad: $name")

  val namer = new Namer[Asset]( _.gameObject.getType.getTypeName )

  val actions = mutable.ListBuffer[(ActorRef, Action)]()
  var replan  = true

  var roi:     Option[RegionOfInterest] = None
  var roiPath: Option[ Seq[Location]  ] = None

  def act = {
    case AddMember( unit ) =>
      logger.debug(s"$name was asked to absorb a new member: $unit")
      val newName = namer.nameFor(unit)
      context.actorOf(Props(new Soldier(owner, newName, unit )), name=newName )

    case NewSurveyRoi( newRoi ) =>
      logger.debug(s"$name is updating the region of interest to $newRoi")
      roi    = Some(newRoi)
      replan = true

    case SubmitActions()   =>
      logger.debug(s"$name was asked to submit all actions for this turn for validation by its parent")
      planActions()
      sender ! nextViableAction

    case ActionSuccess(action, updates) =>
      actions.indexWhere( _._2 == action ) match {
        case -1 =>
          logger.debug(s"$name received ActionSuccess, but thus action was not found in the actions queue! Replanning ...")
          replan = true
        case i  => actions.remove(i)
      }
      updatePlanningFor(updates)
      context.parent ! nextViableAction

    case ActionFailed( action, message, reason ) =>
      logger.warn( s"Action failed ($action) $reason: $message" )

    case ListMetadata() =>
      sender ! Iterable( new MetaData() {
        def title: String = "Survey area"
        def layers: Map[String, LayerRenderer] = Map(
          "Region of interest" -> new RegionRenderer(roi.get, Color.transparent, new Color(1f, 1f, 1f, 0.5f)),
          "Survey path" -> new PolyLineRenderer( actions.map( _._2.asInstanceOf[MoveTo].location ), new DrawStyle(Color.yellow, 3f) )
        )
      } )

    case any =>
      logger.debug(s"Invalid message received: $any")
  }

  def updatePlanningFor( updates: Iterator[Any] ) = for (update <- updates)  update match {
    case UpdateLocation(l) =>
      implicit val game = owner.getController.getGameController.getGame
      val tile: Tile = location2tile(l)
      toMaybe(tile.checkContents()).asOption match {
        case Some( content ) if content.getOwner.isPresent && content.getOwner.get != owner =>
          logger.debug(s"$name is going to replan because an enemy was detected on $tile: $content")
          replan = true
        case _ =>
      }
      cartographer ! UpdateLocation(l)
    case _ =>
  }

  def nextViableAction = {
    logger.trace(s"Finding next action for $name")
    val canDoAction = mutable.Map[ActorRef, Boolean]()
    actions.find( entry => {
      val unit   = entry._1
      val action = entry._2
      canDoAction.getOrElseUpdate( unit, {
        val simulationSuccess =  Await.result(unit ? SimulateAction( action ), timeout.duration).asInstanceOf[ActionResult]
        simulationSuccess match {
          case ActionSuccess(a, _) => true
          case _ => false
        }
      } )
    } ) match {
      case Some(action) =>
        logger.trace(s"Found action for unit $name: $action")
        ValidateAction(action._2, action._1)
      case None =>
        logger.trace(s"No more action possible for $name (queue: $actions)")
        Ready()
    }
  }

  def planActions() = if (replan || actions.isEmpty) {
    logger.info(s"$name is replanning actions ...")
    // Remove all old actions
    actions.clear()

    roi match {
      case Some(region) =>
        val (center, radius) = enclosingCircle()

        val scanRadiusFuture = for (child <- context.children) yield {
          Await.result( for ( asset <- (child ? GetAsset()).mapTo[Asset] ) yield asset.base.getViewRange, timeout.duration)
        }
        val scanRadius =  scanRadiusFuture.max

        // The complete scan path
        val completeSpiralPath = center.spiral(radius, 2*scanRadius+1)

        // Remove tiles where enough information exists
        val remainingSpiralPath = completeSpiralPath.filter( location => {
          val tilesInView = location.range( scanRadius )
          val futureConfidence =
            for {fu <- (cartographer ? tilesInView.map( ResourcesOn(_, ResourceType.FUEL         )).toSeq ).mapTo[ Seq[ResourceCount] ]
                 si <- (cartographer ? tilesInView.map( ResourcesOn(_, ResourceType.SILICON      )).toSeq ).mapTo[ Seq[ResourceCount] ]
                 me <- (cartographer ? tilesInView.map( ResourcesOn(_, ResourceType.METALS       )).toSeq ).mapTo[ Seq[ResourceCount] ]
                 re <- (cartographer ? tilesInView.map( ResourcesOn(_, ResourceType.RARE_ELEMENTS)).toSeq ).mapTo[ Seq[ResourceCount] ] } yield {

              val fuRMS = math.sqrt( fu.foldLeft( 0.0 )( (squares, value) => squares + math.pow(value.confidence, 2) ) / fu.size.toDouble )
              val siRMS = math.sqrt( si.foldLeft( 0.0 )( (squares, value) => squares + math.pow(value.confidence, 2) ) / fu.size.toDouble )
              val meRMS = math.sqrt( me.foldLeft( 0.0 )( (squares, value) => squares + math.pow(value.confidence, 2) ) / fu.size.toDouble )
              val reRMS = math.sqrt( re.foldLeft( 0.0 )( (squares, value) => squares + math.pow(value.confidence, 2) ) / fu.size.toDouble )

              math.sqrt( (fuRMS*fuRMS + siRMS*siRMS + meRMS*meRMS + reRMS*reRMS) / 4.0 )
            }
          val confidence = Await.result(futureConfidence, timeout.duration)
          confidence < 0.9
        } )

        val moveActions = for (child <- context.children) yield {
          val moveActionsFuture = for {
            asset <- (child ? GetAsset()).mapTo[Asset]
            soldier <- (child ? Self()).mapTo[Soldier]
          } yield {
            var position = asset.location
            val actionSequence = ListBuffer[(ActorRef, Action)]()
            for ( surveyLocation <- remainingSpiralPath ) {
              if (!(surveyLocation adjacentTo position)) {
                val solution = new MovementPathfinder(surveyLocation, asset).findPath(position)
                solution._1.path.reverse.foreach(l => actionSequence.append( (child, MoveTo(l)) ) )
                position = surveyLocation
              }
              actionSequence.append( (child, MoveTo(surveyLocation)) )
            }
            actionSequence.result()
          }
          Await.result(moveActionsFuture, timeout.duration)
        }
        actions.appendAll(moveActions.flatten)
        replan = false
      case None =>
    }
  }

  // Returns (center, radius)
  def enclosingCircle() = roi match {
    case Some( region ) if region.tiles.nonEmpty =>
      val points = region.tiles
      val first  = points.head

      var (centerU, centerV) = (0.0, 0.0)
      for (point <- points) {
        centerU = centerU + point.u
        centerV = centerV + point.v
      }
      val center = Location( centerU / points.size.toDouble, centerV / points.size.toDouble, first.h, first.size )

      var radius = -1
      for (point <- points)
        radius = math.max(radius, center δ point)

      (center, radius)
    case _ =>
      ( Location(0,0,0,null), -1)
  }

}

case class NewSurveyRoi( roi: RegionOfInterest )