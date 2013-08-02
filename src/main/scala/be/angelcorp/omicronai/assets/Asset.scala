package be.angelcorp.omicronai.assets

import collection.JavaConverters._
import com.lyndir.omicron.api.model._
import com.lyndir.omicron.api.controller._
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.Conversions._

class Asset( val aiPlayer: Player, val gameObject: GameObject) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def location: Location = gameObject.getLocation

  def observableTiles = gameObject.listObservableTiles(aiPlayer).iterator().asScala

  lazy val mobility = toOption( gameObject.getModule( classOf[MobilityModule] ) )

}
