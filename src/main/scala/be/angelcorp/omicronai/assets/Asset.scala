package be.angelcorp.omicronai.assets

import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model._
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.Conversions._

class Asset( val aiPlayer: Player, val gameObject: GameObject) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def location: Location = gameObject.getLocation

  def observableTiles = gameObject.listObservableTiles(aiPlayer).iterator().asScala

  lazy val mobility = toOption( gameObject.getModule( ModuleType.MOBILITY, 0 ) )

}
