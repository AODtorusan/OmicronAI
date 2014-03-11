package be.angelcorp.omicron.base.bridge

import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import be.angelcorp.omicron.base.Conversions._
import be.angelcorp.omicron.base.{Auth, Location}
import be.angelcorp.omicron.base.Location._
import be.angelcorp.omicron.base.world.WorldBounds

trait Asset {

  def gameObject: IGameObject

  def name:         String

  def owner:        Option[IPlayer]
  def location:     Option[Location]
  def observableTiles: Iterable[Location]
  def modules:      Iterable[IModule]

  def base:         IBaseModule
  def mobility:     Option[IMobilityModule]
  def constructors: Iterable[IConstructorModule]
  def containers:   Iterable[IContainerModule]
  def extractors:   Iterable[IExtractorModule]
  def weapons:      Iterable[IWeaponModule]

  // Cached values for mobility module:
  def costForMovingInLevel(h: Int):   Double
  def costForLevelingToLevel(h: Int): Double
}

class AssetImpl( val auth: Auth, val gameObject: IGameObject) extends Asset {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  private implicit val game = auth.player.getController.getGameController.getGame

  def name                = gameObject.getType.getTypeName

  def owner: Option[IPlayer]     = toMaybe( auth { gameObject.checkOwner()    }).asOption
  def location: Option[Location] = toMaybe( auth { gameObject.checkLocation() }).asOption.map( tile2location )

  def observableTiles     = auth { gameObject.iterateObservableTiles().asScala.map( tile2location ) }

  lazy val modules        = auth { gameObject.listModules().asScala }

  lazy val base           = auth { gameObject.getModule( ModuleType.BASE, 0 ).get()           }
  lazy val mobility       = auth { toOption( gameObject.getModule( ModuleType.MOBILITY, 0 ) ) }
  lazy val constructors   = auth { gameObject.getModules( ModuleType.CONSTRUCTOR ).asScala    }
  lazy val containers     = auth { gameObject.getModules( ModuleType.CONTAINER   ).asScala    }
  lazy val extractors     = auth { gameObject.getModules( ModuleType.EXTRACTOR   ).asScala    }
  lazy val weapons        = auth { gameObject.getModules( ModuleType.WEAPON      ).asScala    }

  private lazy val moveInCosts = auth {
    for( h <- 0 until (game.getLevelSize: WorldBounds).hSize) yield
      mobility.map( _.costForMovingInLevel(Location.int2levelType(h)) ).getOrElse(Double.NaN)
  }

  private lazy val moveToCosts = auth {
    for( h <- 0 until (game.getLevelSize: WorldBounds).hSize) yield
      mobility.map( _.costForLevelingToLevel(Location.int2levelType(h)) ).getOrElse(Double.NaN)
  }

  def costForMovingInLevel(h: Int)   = moveInCosts(h)
  def costForLevelingToLevel(h: Int) = moveToCosts(h)

  override def equals(obj: scala.Any): Boolean = obj match {
    case asset: Asset => asset.gameObject == gameObject
    case _ => false
  }

}
