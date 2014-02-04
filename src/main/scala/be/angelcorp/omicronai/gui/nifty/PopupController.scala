package be.angelcorp.omicronai.gui.nifty

import de.lessvoid.nifty.elements.Element
import de.lessvoid.nifty.Nifty
import de.lessvoid.nifty.controls.{Menu, MenuItemActivatedEvent}
import de.lessvoid.nifty.tools.SizeValue
import org.bushe.swing.event.EventTopicSubscriber
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicronai.gui.GuiController
import be.angelcorp.omicronai.gui.input.{InputHandler, GuiInputEvent, MouseClicked}
import scala.reflect.ClassTag

trait PopupController extends GuiController with InputHandler {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  /** Nifty gui */
  def nifty: Nifty
  /** Generates the content for the default right-click popup menu  */
  def defaultMenu: Iterable[(String, () => Unit)]

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

  def handleInputEvent(event: GuiInputEvent): Boolean = event match {
    case MouseClicked(x, y, 1, 1) => showMenu( defaultMenu ); true
    case _ => false
  }

}

object PopupController {

  def xml[T <: PopupController: Manifest] = {
    <popup id="niftyPopupMenu" childLayout="absolute-inside" controller={manifest[T].getClass.getName} width="100px">
      <interact onClick="closePopup()" onSecondaryClick="closePopup()" onTertiaryClick="closePopup()" />
      <control id="#menu" name="niftyMenu" />
    </popup>
  }

}
