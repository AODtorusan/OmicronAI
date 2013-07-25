package be.angelcorp.omicronai.gui

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import org.newdawn.slick.util.InputAdapter
import org.newdawn.slick.Input._
import collection.mutable

class AiGuiInput(gui: AiGui) extends InputAdapter {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  val downKeys = mutable.Set[Int]()

  override def mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    moveByPx( newx-oldx, newy-oldy )
  }

  override def mouseWheelMoved(change: Int) {
    gui.scaleBy(change / 500.0f)
  }

  override def keyPressed(key: Int, c: Char) = {
    downKeys.add(key)
    key match {
      case KEY_UP       => moveByPx( 0,  10)
      case KEY_DOWN     => moveByPx( 0, -10)
      case KEY_LEFT     => moveByPx(  10, 0)
      case KEY_RIGHT    => moveByPx( -10, 0)
      case KEY_ADD      => gui.scaleBy( 0.1f)
      case KEY_SUBTRACT => gui.scaleBy(-0.1f)
      case KEY_DELETE   =>
        gui.scaleTo(0.5f)
        gui.moveTo(0, 0)


      case KEY_F4 if isDown(KEY_LALT) || isDown(KEY_RALT) => System.exit(0)
      case _ =>
    }
  }

  override def keyReleased(key: Int, c: Char) {
    downKeys.remove(key)
    key match {
      case _ =>
    }
  }

  def moveByPx(deltaX: Float, deltaY: Float) =
    gui.moveBy( deltaX/gui.scale, deltaY/gui.scale )

  def isDown(key: Int) = downKeys.contains(key)

}
