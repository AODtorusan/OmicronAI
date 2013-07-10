package be.angelcorp.omicronai

import com.typesafe.config.{ConfigSyntax, ConfigParseOptions, Config, ConfigFactory}

class Settings( config: Config ) {

  val ai = new AISettings( config.getConfig("ai") )

}

class AISettings(config: Config) {
  val name = config.getString( "name" )
}

object Settings {
  val settings = new Settings( ConfigFactory.load() )
}
