package be.angelcorp.omicron.base.gui

import java.io.ByteArrayInputStream
import scala.io.Source
import scala.xml.Elem
import de.lessvoid.nifty.screen.ScreenController
import de.lessvoid.nifty.Nifty
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

trait GuiScreen {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val screenId: String

  val screenType: ScreenType

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

sealed abstract class ScreenType
/** Screen that completely fill the monitor (game not visible) */
object ScreenFill    extends ScreenType
/** Screen that partially overlays the monitor (game still visible) */
object ScreenOverlay extends ScreenType
