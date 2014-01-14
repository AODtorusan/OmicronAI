package be.angelcorp.omicronai.gui.input

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import org.newdawn.slick.Input._
import be.angelcorp.omicronai.gui.AiGui

class AiGuiInput(gui: AiGui) extends InputHandler {
  private val logger = Logger( LoggerFactory.getLogger( getClass ) )

  private var altDown = false

  def handleInputEvent(event: GuiInputEvent): Boolean = event match {
    case MouseMoved(_, _, x, y)               => updateTileHover(x, y); false
    case MouseDragged(fromX, fromY, toX, toY, 0) => moveByPx( toX-fromX, toY-fromY); true
    case MouseWheelMoved(_,_,delta)           => gui.view.scaleBy(delta / 500.0f); true

    case KeyPressed(KEY_UP, _, _, false, _)   => moveByPx(   0,   10); true
    case KeyPressed(KEY_UP, _, _, true, _)    => moveByPx(   0,  100); true
    case KeyPressed(KEY_DOWN, _, _, false, _) => moveByPx(   0,  -10); true
    case KeyPressed(KEY_DOWN, _, _, true, _)  => moveByPx(   0, -100); true
    case KeyPressed(KEY_LEFT, _, _, false, _) => moveByPx(   10,   0); true
    case KeyPressed(KEY_LEFT, _, _, true, _)  => moveByPx(  100,   0); true
    case KeyPressed(KEY_RIGHT, _, _, false, _)=> moveByPx(  -10,   0); true
    case KeyPressed(KEY_RIGHT, _, _, true, _) => moveByPx( -100,   0); true

    case KeyPressed(KEY_ADD, _, _, _, _)      => gui.view.scaleBy( 0.1f); true
    case KeyPressed(KEY_SUBTRACT, _, _, _, _) => gui.view.scaleBy(-0.1f); true
    case KeyPressed(KEY_DELETE, _, _, _, _)   => gui.view.scaleTo(0.5f); gui.view.moveTo(0, 0); true
    case KeyPressed(KEY_F4, _, _, _, _) if altDown  => System.exit(0); true

    case KeyPressed(KEY_LALT, _, _, _, _)     => altDown = true; false
    case KeyReleased(KEY_LALT, _, _, _, _)    => altDown = false; false

    case _ => false
  }

  def updateTileHover(x: Int, y: Int) {
    val gameXY = gui.view.pixelToOpengl(x, y)
    val coor =   gui.view.openglToTile( gameXY._1, gameXY._2 )
    gui.hoverTile = Some( coor )
  }

  /** Move the gui viewport by a specific amount pixels */
  def moveByPx(deltaX: Float, deltaY: Float) =
    gui.view.moveBy( deltaX/gui.view.scale, deltaY/gui.view.scale )

}
