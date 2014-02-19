package be.angelcorp.omicron.gui

import org.slf4j.bridge.SLF4JBridgeHandler
import java.nio.file.Paths

object Main extends App {

  SLF4JBridgeHandler.install()
  bootstrap()
  AiGui.start()

  def bootstrap() {
    // Copy required LWJGL libraries
    Platform.loadLWJGL()

    // Has the user specified his own configuration ?
    if (!Seq("config.resource", "config.file", "config.url").exists( k => System.getProperty(k) != null )) {
      // Create a config file if missing
      System.setProperty("config.file", "application.conf")
      val forceNewConfig = Option(System.getProperty("be.angelcorp.omicon.gui.newconfig")).exists(_.toBoolean)
      Platform.extractIfMissingOr( "application.conf.template", Paths.get("application.conf"), forceNewConfig )
    }
  }

}
