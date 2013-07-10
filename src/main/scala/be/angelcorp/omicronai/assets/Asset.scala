package be.angelcorp.omicronai.assets

import collection.JavaConverters._
import akka.actor.{ActorContext, Props, ActorRef}

import com.lyndir.omnicron.api.model.{Player, GameObject}
import com.lyndir.omnicron.api.controller.MobilityModule
import be.angelcorp.omicronai.Conversions._
import be.angelcorp.omicronai.agents.{ExecuteOrders, Agent, DeafAgent}
import be.angelcorp.omicronai.Location
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory


class Asset( val aiPlayer: Player, val gameObject: GameObject) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def location: Location = gameObject.getLocation

  def observableTiles = gameObject.listObservableTiles(aiPlayer).iterator().asScala

  lazy val mobility = toOption( gameObject.getModule( classOf[MobilityModule] ) )

}
