package be.angelcorp.omicronai.configuration

import collection.mutable
import com.typesafe.config.{ConfigException, Config, ConfigFactory}
import org.slf4j.LoggerFactory

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
  private val cache = mutable.Map[Class[_], Boolean]()

  val defaultAuto = config.getBoolean( "defaultAuto" )
  def forwardOnFor( m: Any ) = cache.getOrElseUpdate( m.getClass, {
    try {
      config.getBoolean( m.getClass.getName )
    } catch {
      case e: ConfigException.Missing => false
    }
  } )
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
