package be.angelcorp.omicronai.gui.input

import scala.collection.mutable
import de.lessvoid.nifty.slick2d.input.AbstractSlickInputSystem
import de.lessvoid.nifty.slick2d.input.events._
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

class InputSystem(val inputHandlers: mutable.SynchronizedBuffer[InputHandler]) extends AbstractSlickInputSystem {
  private val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def this() = this( new mutable.ArrayBuffer[InputHandler] with mutable.SynchronizedBuffer[InputHandler] )

  /**
   * Pass the event to the `InputHandler`s until one consumes the event
   */
  def handleInputEvent(niftyEvent: InputEvent) {
    val event = niftyEvent match {
      case e: KeyboardEventReleased => KeyReleased(e.getKey, e.getCharacter, e.isKeyDown, e.isShiftDown, e.isControlDown)
      case e: KeyboardEventPressed  => KeyPressed(e.getKey, e.getCharacter, e.isKeyDown, e.isShiftDown, e.isControlDown)
      case e: MouseEventWheelMoved  => MouseWheelMoved(e.getX, e.getY, e.getDelta)
      case e: MouseEventPressed     => MousePressed( e.getX, e.getY, e.getButton)
      case e: MouseEventDragged     => MouseDragged( e.getX, e.getY, e.getTargetX, e.getTargetY, e.getButton)
      case e: MouseEventClicked     => MouseClicked( e.getX, e.getY, e.getButton, e.getCount)
      case e: MouseEventReleased    => MouseReleased(e.getX, e.getY, e.getButton)
      case e: MouseEventMoved       => MouseMoved(e.getX, e.getY, e.getTargetX, e.getTargetY)
    }
    //logger.trace( s"Input event: $event" )
    inputHandlers.exists( _.handleInputEvent(event) )
  }

}

sealed abstract class GuiInputEvent
case class KeyReleased(keyId: Int, keyChar: Char, keyDown: Boolean, shiftDown: Boolean, controlDown: Boolean) extends  GuiInputEvent
case class KeyPressed(keyId: Int, keyChar: Char, keyDown: Boolean, shiftDown: Boolean, controlDown: Boolean) extends  GuiInputEvent
case class MouseWheelMoved(mouseX: Int, mouseY: Int, delta: Int) extends  GuiInputEvent
case class MousePressed(x: Int, y: Int, mouseButton: Int) extends  GuiInputEvent
case class MouseDragged(startX: Int, startY: Int, endX: Int, endY: Int, mouseButton: Int) extends  GuiInputEvent
case class MouseClicked(x: Int, y: Int, mouseButton: Int, clickCount: Int) extends  GuiInputEvent
case class MouseReleased(x: Int, y: Int, mouseButton: Int) extends  GuiInputEvent
case class MouseMoved(startX: Int, startY: Int, endX: Int, endY: Int) extends  GuiInputEvent
