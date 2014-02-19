package be.angelcorp.omicron.base.gui.nifty

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.Props
import de.lessvoid.nifty.elements.Element
import de.lessvoid.nifty.Nifty
import de.lessvoid.nifty.controls.{Menu, MenuItemActivatedEvent}
import de.lessvoid.nifty.tools.SizeValue
import org.bushe.swing.event.EventTopicSubscriber
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicron.base.gui.GuiController
import be.angelcorp.omicron.base.gui.input.{MouseClicked, InputHandler}

trait PopupController extends GuiController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  /** Nifty gui */
  def nifty: Nifty
  /** Generates the content for the default right-click popup menu  */
  def defaultMenu: Iterable[(String, () => Unit)]
  /** OpenGL ExecutionContext */
  def openGL: ExecutionContext

  private var popup: Element = null

  private val menuClickHandler = new EventTopicSubscriber[MenuItemActivatedEvent[() => Unit ]] {
    def onEvent(id: String, event: MenuItemActivatedEvent[() => Unit ]) {
      nifty.closePopup(popup.getId)
      val callback = event.getItem
      logger.info(s"Clicked on $id; $callback")
      callback()
    }
  }

  def showMenu( entries: Iterable[(String, () => Unit)] ) {
    popup = nifty.createPopup("niftyPopupMenu")
    val popupMenu  = popup.findNiftyControl("#menu", classOf[Menu[ () => Unit ]])
    popupMenu.setWidth(new SizeValue("200px"))

    for( (entryString, callback) <- entries )
      popupMenu.addMenuItem( entryString, callback )
    popupMenu.addMenuItem("Close", () => {} )

    nifty.subscribe( nifty.getCurrentScreen, popupMenu.getId, classOf[MenuItemActivatedEvent[() => Unit ]], menuClickHandler )
    nifty.showPopup( nifty.getCurrentScreen, popup.getId, null )
  }

  def inputHandler = Props( new InputHandler {
    override def receive = {
      case MouseClicked(x, y, 1, 1) =>
        Future {
          showMenu( defaultMenu )
        }(openGL /*exec in opengl*/)
    }
  } )

}

object PopupController {

  def xml[T <: PopupController: Manifest] = {
    <popup id="niftyPopupMenu" childLayout="absolute-inside" controller={manifest[T].getClass.getName} width="100px">
      <interact onClick="closePopup()" onSecondaryClick="closePopup()" onTertiaryClick="closePopup()" />
      <control id="#menu" name="niftyMenu" />
    </popup>
  }

}
