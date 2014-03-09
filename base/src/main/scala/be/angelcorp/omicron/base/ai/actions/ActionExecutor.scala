package be.angelcorp.omicron.base.ai.actions

import scala.Some
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.util.{Try, Failure, Success}
import java.util.concurrent.TimeUnit
import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.model.IConstructorModule.IConstructionSite
import be.angelcorp.omicron.base.{Auth, Present, Direction, Location}
import be.angelcorp.omicron.base.Conversions._
import be.angelcorp.omicron.base.ai.AI
import be.angelcorp.omicron.base.bridge.{AssetImpl, Asset}
import be.angelcorp.omicron.base.world.{KnownState, WorldState, LocationState, ReloadReady}

trait ActionExecutor {
  implicit val timeout: Timeout = Duration(1, TimeUnit.MINUTES)

  protected def auth: Auth

  implicit def game: Game
  implicit def executionContext: ExecutionContext

  def world: ActorRef

  private def haltTheWorld[T](f: => Try[T] ): Try[T] = {
    waitForWorld()
    val result = try { f } catch { case e: Throwable => Failure(e) }
    waitForWorld()
    result
  }

  private def haltTheWorld[T](f: => T ): T = {
    waitForWorld()
    val result = f
    waitForWorld()
    result
  }

  private def waitForWorld() {
    Await.result( ask(world, ReloadReady()).mapTo[Boolean], timeout.duration)
  }

  def attack( asset: Asset, weaponModule: WeaponModule, target: Location): Future[Try[Unit]] =
    Future { haltTheWorld (
      asset.weapons.find( _ == weaponModule ) match {
        case Some(module) =>
          if ( auth { module.fireAt( target ) } )
            Success()
          else
            Failure( new ActionExecutionException(s"Asset $asset could not successfully fire at $target with $module", Never) )
        case None =>
          Failure( MissingModule( asset, weaponModule.getType ) )
      }
    ) }

  def move( asset: Asset, path: Seq[Location]): Future[Try[Unit]] = haltTheWorld {
    if (path.length == 0)
      Future.successful( Success() )
    else if (asset.location.get != path.head)
      Future.successful( Failure( TooFar(asset, path.head, 0) ) )
    else {
      asset.mobility match {
        case Some(module) =>
          path.drop(1).foldLeft(Future.successful(Success()): Future[Try[Unit]])( (success, target) => success.flatMap( {
            case Success(_) => auth {
              val action = module.movement( target )
              if (action.isPossible) {
                waitForWorld()
                action.execute()
                Future.successful( Success() )
              } else {
                (world ? LocationState(target)).mapTo[WorldState].map( {
                  case KnownState(_, Some(obj), _) =>
                    obj.mobility match {
                      case Some( _ ) => Failure( BlockedLocation(target, NextTurn) )
                      case None      => Failure( BlockedLocation(target, Never   ) )
                    }
                  case _ =>
                    val origin = asset.location.get
                    val cost = (math.abs(origin δu target) + math.abs(origin δv target)) * asset.costForMovingInLevel(origin.h) + (origin δh target) * asset.costForLevelingToLevel( target.h )
                    val available = auth { module.getRemainingSpeed }
                    if (available < cost)
                      Failure( OutOfMovementPoints(asset, target, cost, available) )
                    else
                      Failure(new ActionExecutionException(s"Cannot move asset $asset to $target", Never))
                } )
              } }
            case f => Future.successful( f )
          } ) )
        case None =>
          Future.successful( Failure(MissingModule(asset, PublicModuleType.MOBILITY)) )
      }
    }
  }

  def move( asset: Asset, direction: Direction): Future[Try[Unit]] = Future {
    haltTheWorld (
      asset.mobility match {
        case Some(module) =>
          asset.location.get neighbour direction match {
            case Some(target) =>
              waitForWorld()
              val action = module.movement( target )
              if (action.isPossible) {
                action.execute()
                Success()
              } else {
                Failure(new ActionExecutionException(s"Cannot move asset $asset to $target", Never))
              }
            case _ => Failure( OutOfMap( asset.location.get, direction ) )
          }
        case None =>
          Failure( MissingModule(asset, PublicModuleType.MOBILITY) )
      }
    )
  }

  def constructionStart( builder: Asset, constructionType: UnitType, destination: Location ): Future[Try[Asset]] = Future {
    haltTheWorld (
      if (builder.location.get adjacentTo destination )
        builder.constructors.headOption match {
          case Some(module) =>
            auth {
              val oldTarget = module.getTarget
              val site      = module.schedule( constructionType, destination )
              if (oldTarget != null) module.setTarget(oldTarget)
              Success(new AssetImpl(auth, site))
            }
          case _ =>
            Failure( MissingModule(builder, PublicModuleType.CONSTRUCTOR) )
        }
      else Failure( TooFar(builder, destination, 1) )
    )
  }

  def constructionAssist( builder: Asset, site: Asset ): Future[Try[Unit]] = Future {
    haltTheWorld (
      if (builder.constructors.isEmpty)
        Failure( MissingModule(builder, PublicModuleType.CONSTRUCTOR) )
      else {
        site.location match {
          case Some(loc) =>
            if (loc adjacentTo builder.location.get) {
              builder.constructors.foreach( _.setTarget(site.gameObject) )
              Success()
            } else
              Failure( TooFar(builder, loc, 1) )
          case _ =>
            Failure( InFogOfWar(s"Cannot get the location of the target work site of $site") )
        }
      }
    )
  }

}

sealed abstract class RetryHint
object Now      extends RetryHint
object NextTurn extends RetryHint
object Never    extends RetryHint

class ActionExecutionException(msg: String, val retryHint: RetryHint, cause: Throwable = null) extends Exception(msg, cause)
case class MissingModule( unit: Asset, typ: PublicModuleType[_<:IModule], cause: Throwable = null  )
  extends ActionExecutionException(s"$unit is missing a required module; $typ", Never, cause)

case class TooFar( unit: Asset, targetLocation: Location, maxDistance: Int, cause: Throwable = null  )
  extends ActionExecutionException(s"$unit is too far from $targetLocation to perform the action. distance=${unit.location.get δ targetLocation}, maximum=$maxDistance", Never, cause)

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
