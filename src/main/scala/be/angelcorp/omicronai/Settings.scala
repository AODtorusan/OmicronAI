package be.angelcorp.omicronai

import com.typesafe.config.{ConfigSyntax, ConfigParseOptions, Config, ConfigFactory}
import org.slf4j.LoggerFactory

class Settings( config: Config ) {

  val ai = new AISettings( config.getConfig("ai") )
  val pathfinder = new PathfinderSettings( config.getConfig("pathfinder") )

}

class AISettings(config: Config) {
  val name       = config.getString( "name" )
  val supervisor = new AiSupervisorSettings( config.getConfig( "supervisor" ) )
}

class AiSupervisorSettings(config: Config) {
  val onNewTurn        = config.getBoolean( "onNewTurn" )
  val onValidateAction = config.getBoolean( "onValidateAction" )
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

object Settings {
  val settings = new Settings( ConfigFactory.load() )
}
