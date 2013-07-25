package be.angelcorp.omicronai.assets

import collection.JavaConverters._

import com.lyndir.omicron.api.model.{Player, GameObject}
import com.lyndir.omicron.api.controller.MobilityModule
import be.angelcorp.omicronai.Conversions._
import be.angelcorp.omicronai.Location
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import collection.mutable

class Asset( val aiPlayer: Player, val gameObject: GameObject) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val name = {
    val count = Asset.unitCount.getOrElse( gameObject.getTypeName, 1 )
    Asset.unitCount.update( gameObject.getTypeName, count + 1 )
    s"${gameObject.getTypeName} $count"
  }

  def location: Location = gameObject.getLocation

  def observableTiles = gameObject.listObservableTiles(aiPlayer).iterator().asScala

  lazy val mobility = toOption( gameObject.getModule( classOf[MobilityModule] ) )

  override def toString = name
}

object Asset {

  val unitCount = mutable.Map[String, Int]()

}
