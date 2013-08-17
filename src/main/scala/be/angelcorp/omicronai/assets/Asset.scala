package be.angelcorp.omicronai.assets

import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.Conversions._

class Asset( val owner: Player, val gameObject: GameObject) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def location: Location = gameObject.getLocation

  def observableTiles = gameObject.listObservableTiles().iterator().asScala

  lazy val base       = gameObject.getModule( ModuleType.BASE, 0 ).get()

  lazy val constructor= toOption( gameObject.getModule( ModuleType.CONSTRUCTOR, 0 ) )
  lazy val container  = toOption( gameObject.getModule( ModuleType.CONTAINER, 0 ) )
  lazy val extractor  = toOption( gameObject.getModule( ModuleType.EXTRACTOR, 0 ) )
  lazy val mobility   = toOption( gameObject.getModule( ModuleType.MOBILITY, 0 ) )

  lazy val weapons    = gameObject.getModules( ModuleType.WEAPON ).asScala


}
