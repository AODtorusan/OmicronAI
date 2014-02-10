package be.angelcorp.omicronai.assets

import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.Conversions._

trait Asset {

  def owner:      Player
  def gameObject: IGameObject

  def location: Location
  def observableTiles: Iterable[Location]

  def base:         IBaseModule
  def mobility:     Option[IMobilityModule]
  def constructors: Iterable[IConstructorModule]
  def containers:   Iterable[IContainerModule]
  def extractors:   Iterable[IExtractorModule]
  def weapons:      Iterable[IWeaponModule]

}

class AssetImpl( val owner: Player, val gameObject: IGameObject) extends Asset {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  private implicit val game = owner.getController.getGameController.getGame

  def location: Location  = gameObject.checkLocation().get()

  def observableTiles     = gameObject.listObservableTiles().asScala.map( tile => Location.tile2location( tile ) )

  lazy val base           = gameObject.getModule( ModuleType.BASE, 0 ).get()
  lazy val mobility       = toOption( gameObject.getModule( ModuleType.MOBILITY, 0 ) )
  lazy val constructors   = gameObject.getModules( ModuleType.CONSTRUCTOR ).asScala
  lazy val containers     = gameObject.getModules( ModuleType.CONTAINER   ).asScala
  lazy val extractors     = gameObject.getModules( ModuleType.EXTRACTOR   ).asScala
  lazy val weapons        = gameObject.getModules( ModuleType.WEAPON      ).asScala

  override def equals(obj: scala.Any): Boolean = obj match {
    case asset: Asset => asset.gameObject == gameObject
    case _ => false
  }

}
