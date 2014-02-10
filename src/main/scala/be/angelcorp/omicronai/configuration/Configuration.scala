package be.angelcorp.omicronai.configuration

import scala.collection.mutable
import scala.collection.JavaConverters._
import com.typesafe.config.{ConfigException, Config, ConfigFactory}
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.configuration.ConfigHelpers._
import akka.actor.{ActorPath, ActorRef}
import scala.util.matching.Regex

class Configuration( config: Config ) {
  val ai = new AISettings( config.getConfig("ai") )
  val gui = new GuiSettings( config.getConfig("gui") )
  val pathfinder = new PathfinderSettings( config.getConfig("pathfinder") )

  val graphics = GraphicsSettings( config.getConfig("graphics") )
}

object Configuration {
  val config = new Configuration( ConfigFactory.load() )
}

class AISettings(config: Config) {
  val engine      = config.getString( "engine" )
  val name        = config.getString( "name" )
  val supervisor  = new AiSupervisorSettings( config.getConfig( "supervisor" ) )
  val messageTimeout = config.getDouble("messageTimeout")
}

class AiSupervisorSettings(config: Config) {
  private val autoCache = mutable.Map[ActorPath, Boolean]()

  val pausedActors       = config.getOptionalStringList( "pausedActors" ).map(_.asScala).getOrElse(Nil).map( s => new Regex(s).pattern )
  val supervisedMessages = config.getOptionalStringList( "supervisedMessages" ).map(_.asScala).getOrElse(Nil).map( s => Class.forName(s) )

  def startActorPaused( a: ActorPath ) = autoCache.getOrElseUpdate( a,
    pausedActors.exists( p => p.matcher(a.name).matches() )
  )

  def isMessagesSupervised( m: Any ) = supervisedMessages.contains(m.getClass)

}

class GuiSettings(config: Config) {
  val messageTimeout = config.getDouble("messageTimeout")
}

class PathfinderSettings( config: Config ) {

  val layerChangePenalty = config.getDouble( "layerChangePenalty" )
  val groundPenalty      = config.getDouble( "groundPenalty" )
  val skyPenalty         = config.getDouble( "skyPenalty" )
  val spacePenalty       = config.getDouble( "spacePenalty" )

  def layerPenalty(h: Int): Double = h match {
    case 0 => groundPenalty
    case 1 => skyPenalty
    case 2 => spacePenalty
    case _ => LoggerFactory.getLogger( getClass ).warn("Could not find pathfinder layer penalty for layer {}, assuming 0", h); 0.0
  }

}
