package be.angelcorp.omicronai.assets

import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.Conversions._

class Asset( val owner: Player, val gameObject: IGameObject) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def objectType          = gameObject.getType
  def location: Location  = gameObject.checkLocation().get()

  def observableTiles     = gameObject.listObservableTiles().iterator().asScala

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
