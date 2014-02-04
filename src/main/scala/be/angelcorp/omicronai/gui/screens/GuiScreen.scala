package be.angelcorp.omicronai.gui.screens

import de.lessvoid.nifty.screen.{ScreenController, Screen}
import de.lessvoid.nifty.Nifty
import be.angelcorp.omicronai.gui.AiGui
import scala.xml.Elem
import java.io.ByteArrayInputStream
import scala.io.Source
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

trait GuiScreen {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val name: String

  def loadNiftyXml(nifty: Nifty, xml: Elem, controller: ScreenController*) {
    val xmlStream = new ByteArrayInputStream( xml.toString().getBytes )

    // Try to validate the XML
    if (logger.underlying.isDebugEnabled) {
      logger.debug(s"Validating nifty xml: ${Source.fromInputStream(xmlStream).getLines().foldLeft("")( (msg, line) => msg+'\n'+line )}")
      xmlStream.reset()
      try {
        nifty.validateXml( xmlStream )
      } catch {
        case e: Exception => logger.warn("Could not completely validate the previous nifty xml file", e)
      } finally {
        xmlStream.reset()
      }
    }

    // Attach the passed controllers and load the xml
    controller.foreach( nifty.registerScreenController(_) )
    nifty.addXml( xmlStream )
  }


}
