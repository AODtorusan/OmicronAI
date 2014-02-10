package be.angelcorp.omicronai.ai

import java.util.concurrent.TimeUnit
import scala.{concurrent, Some}
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration.Duration
import scala.util.{Try, Failure,  Success}
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.IConstructorModule.IConstructionSite
import be.angelcorp.omicronai.{Direction, Present, Location}
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.Conversions._
import be.angelcorp.omicronai.world.{KnownState, WorldState, LocationState, ReloadReady}

trait ActionExecutor {
  implicit val timeout: Timeout = Duration(1, TimeUnit.MINUTES)
  implicit def game: Game
  implicit def executionContext: ExecutionContext

  def world: ActorRef

  private def haltTheWorld[T](f: => T ) = {
    waitForWorld()
    val result = f
    waitForWorld()
    result
  }

  private def waitForWorld() {
    Await.result( ask(world, ReloadReady()).mapTo[Boolean], timeout.duration)
  }

  protected[ai] def attack( asset: Asset, weaponModule: WeaponModule, target: Location): Future[Try[Unit]] =
    haltTheWorld ( Future.successful {
      asset.weapons.find( _ == weaponModule ) match {
        case Some(module) =>
          if (module.fireAt( target ))
            Success()
          else
            Failure( new ActionExecutionException(s"Asset $asset could not successfully fire at $target with $module", Never) )
        case None =>
          Failure( MissingModule( asset, weaponModule.getType ) )
      }
    } )

  protected[ai] def move( asset: Asset, path: Seq[Location]): Future[Try[Unit]] = haltTheWorld {
    if (path.length == 0)
      Future.successful( Success() )
    else if (asset.location != path.head)
      Future.successful( Failure( TooFar(asset, path.head, 0) ) )
    else {
      asset.mobility match {
        case Some(module) =>
          path.drop(1).foldLeft(Future.successful(Success()): Future[Try[Unit]])( (success, target) => success.flatMap( {
            case Success(_) =>
              val action = module.movement( target )
              if (action.isPossible) {
                waitForWorld()
                action.execute()
                waitForWorld()
                Future.successful( Success() )
              } else {
                (world ? LocationState(target)).mapTo[WorldState].map( {
                  case KnownState(_, Some(obj), _) =>
                    toOption( obj.getModule( PublicModuleType.MOBILITY, 0 ) ) match {
                      case Some( _ ) => Failure( BlockedLocation(target, NextTurn) )
                      case None      => Failure( BlockedLocation(target, Never   ) )
                    }
                  case _ =>
                    val origin = asset.location
                    val cost = (math.abs(origin δu target) + math.abs(origin δv target)) * module.costForMovingInLevel( Location.int2level(origin.h).getType ) +
                      (origin δh target) * module.costForLevelingToLevel( Location.int2level(target.h).getType )
                    val available = module.getRemainingSpeed
                    if (available < cost)
                      Failure( OutOfMovementPoints(asset, target, cost, available) )
                    else
                      Failure(new ActionExecutionException(s"Cannot move asset $asset to $target", Never))
                } )
              }
            case f => Future.successful( f )
          } ) )
        case None =>
          Future.successful( Failure(MissingModule(asset, PublicModuleType.MOBILITY)) )
      }
    }
  }

  protected[ai] def move( asset: Asset, direction: Direction): Future[Try[Unit]] = haltTheWorld ( Future.successful {
    asset.mobility match {
      case Some(module) =>
        asset.location neighbour direction match {
          case Some(target) =>
            waitForWorld()
            val action = module.movement( target )
            if (action.isPossible) {
              action.execute()
              Success()
            } else {
              Failure(new ActionExecutionException(s"Cannot move asset $asset to $target", Never))
            }
          case _ => Failure( OutOfMap( asset.location, direction ) )
        }
      case None =>
        Failure( MissingModule(asset, PublicModuleType.MOBILITY) )
    }
  } )

  protected[ai] def constructionStart( builder: Asset, constructionType: UnitType, destination: Location ): Future[Try[IConstructionSite]] = haltTheWorld ( Future.successful{
    if (builder.location adjacentTo destination )
      builder.constructors.headOption match {
        case Some(module) =>
          val oldTarget = module.getTarget
          val site      = module.schedule( constructionType, destination )
          module.setTarget(oldTarget)
          Success(site)
        case _ =>
          Failure( MissingModule(builder, PublicModuleType.CONSTRUCTOR) )
      }
    else Failure( TooFar(builder, destination, 1) )
  })

  protected[ai] def constructionAssist( builder: Asset, site: IGameObject ): Future[Try[Unit]] = haltTheWorld ( Future.successful{
    if (builder.constructors.isEmpty)
      Failure( MissingModule(builder, PublicModuleType.CONSTRUCTOR) )
    else {
      toMaybe(site.checkLocation()) match {
        case Present(loc) =>
          val siteLocation: Location = loc
          if (siteLocation adjacentTo builder.location) {
            builder.constructors.foreach( _.setTarget(site) )
            Success()
          } else
            Failure( TooFar(builder, siteLocation, 1) )
        case _ =>
          Failure( InFogOfWar(s"Cannot get the location of the target work site of $site") )
      }
    }
  })

}

sealed abstract class RetryHint
object Now      extends RetryHint
object NextTurn extends RetryHint
object Never    extends RetryHint

class ActionExecutionException(msg: String, val retryHint: RetryHint, cause: Throwable = null) extends Exception(msg, cause)
case class MissingModule( unit: Asset, typ: PublicModuleType[_<:IModule], cause: Throwable = null  )
  extends ActionExecutionException(s"$unit is missing a required module; $typ", Never, cause)

case class TooFar( unit: Asset, targetLocation: Location, maxDistance: Int, cause: Throwable = null  )
  extends ActionExecutionException(s"$unit is too far from $targetLocation to perform the action. distance=${unit.location δ targetLocation}, maximum=$maxDistance", Never, cause)

case class BlockedLocation( target: Location, retry: RetryHint, cause: Throwable = null  )
  extends ActionExecutionException(s"Target location $target is blocked.", retry, cause)

case class OutOfMap( from: Location, direction: Direction, cause: Throwable = null  )
  extends ActionExecutionException(s"Target tile is not in the game map (from $from, towards the $direction)", Never, cause)

case class OutOfMovementPoints( asset: Asset, destination: Location, required: Double, available: Double, cause: Throwable = null )
  extends ActionExecutionException(s"$asset cannot move to $destination, out of movement points ($available < $required)", NextTurn, cause)

case class TimedOut( msg: String, cause: Throwable = null  )
  extends ActionExecutionException(msg, Now, cause)

case class InFogOfWar( msg: String, cause: Throwable = null )
  extends ActionExecutionException(msg, NextTurn, cause)
