package be.angelcorp.omicron.base.configuration

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.util.matching.Regex
import akka.actor.{ActorPath, ActorRef}
import org.lwjgl.input.Keyboard
import org.slf4j.LoggerFactory
import com.typesafe.config.{ConfigException, Config, ConfigFactory}
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicron.base.configuration.ConfigHelpers._
import be.angelcorp.omicron.base.gui.input.{KeyReleased, GuiInputEvent, KeyPressed}

class Configuration( config: Config ) {
  val ai = new AISettings( config.getConfig("ai") )
  val pathfinder = new PathfinderSettings( config.getConfig("pathfinder") )

  val noai = new NoAISettings( config.getConfig("noai") )

  val gui = new GuiSettings( config.getConfig("gui") )
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

class NoAISettings(config: Config) {
  val endTurn               = InputSettings(config, "keybindings.endTurn")
  val updateOrConfirmAction = InputSettings(config, "keybindings.updateOrConfirmAction")
  val centerView            = InputSettings(config, "keybindings.centerView")
  val nextUnit              = InputSettings(config, "keybindings.nextUnit")
  val previousUnit          = InputSettings(config, "keybindings.previousUnit")
}

class GuiSettings(config: Config) {
  val messageTimeout = config.getDouble("messageTimeout")

  val cameraNorth     = InputSettings(config, "keybindings.cameraNorth")
  val cameraSouth     = InputSettings(config, "keybindings.cameraSouth")
  val cameraEast      = InputSettings(config, "keybindings.cameraEast")
  val cameraWest      = InputSettings(config, "keybindings.cameraWest")
  val cameraNorthFast = InputSettings(config, "keybindings.cameraNorthFast")
  val cameraSouthFast = InputSettings(config, "keybindings.cameraSouthFast")
  val cameraEastFast  = InputSettings(config, "keybindings.cameraEastFast")
  val cameraWestFast  = InputSettings(config, "keybindings.cameraWestFast")
  val cameraOut       = InputSettings(config, "keybindings.cameraOut")
  val cameraOutFast   = InputSettings(config, "keybindings.cameraOutFast")
  val cameraIn        = InputSettings(config, "keybindings.cameraIn")
  val cameraInFast    = InputSettings(config, "keybindings.cameraInFast")
  val cameraReset     = InputSettings(config, "keybindings.cameraReset")
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

object InputSettings {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  type InputFilter = GuiInputEvent => Boolean

  def apply( cfg: Config, key: String) =
    cfg.getOptionalConfig(key).map( parseAction ).getOrElse(
      cfg.getOptionalString(key).map( parseAction ).getOrElse( {
        logger.warn(s"Invalid keybinding configuration for key $key in $cfg")
        parseAction("KEY_NONE")
      } )
    )

  def keyCode( key: String ) = {
    val keyCode = Keyboard.getKeyIndex( key )
    if (keyCode == Keyboard.KEY_NONE)
      logger.warn(s"Invalid keybinding, the key ($key) is not defined by lwjgl. For a list of valid key's see the 'KEY_???' fields in org.lwjgl.input.Keyboard without the 'KEY_' prefix.")
    keyCode
  }

  def parseAction( key: String ): InputFilter =
    keyReleased(keyCode(key), shiftDown = false, controlDown = false, altDown = false)

  def parseAction( cfg: Config ): InputFilter = {
    val key         = keyCode(cfg.getString("key"))
    val shiftDown   = cfg.getOptionalBoolean("shiftDown")   getOrElse false
    val controlDown = cfg.getOptionalBoolean("controlDown") getOrElse false
    val altDown     = cfg.getOptionalBoolean("altDown")     getOrElse false
    cfg.getOptionalString("trigger") match {
      case Some("KeyPressed")      => keyPressed( key, shiftDown, controlDown, altDown)
      case Some("KeyReleased") | _ => keyReleased(key, shiftDown, controlDown, altDown)
    }
  }

  def keyReleased(keyId: Int, shiftDown: Boolean, controlDown: Boolean, altDown: Boolean): InputFilter = {
    case KeyReleased(`keyId`, `shiftDown`, `controlDown`, `altDown`) => true
    case _ => false
  }
  def keyPressed(keyId: Int, shiftDown: Boolean, controlDown: Boolean, altDown: Boolean): InputFilter = {
    case KeyPressed(`keyId`, `shiftDown`, `controlDown`, `altDown`) => true
    case _ => false
  }

}
