package be.angelcorp.omicronai.gui

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import org.newdawn.slick.util.InputAdapter
import org.newdawn.slick.Input._
import collection.mutable
import be.angelcorp.omicronai.HexTile

class AiGuiInput(gui: AiGui) extends InputAdapter {
  private val logger = Logger( LoggerFactory.getLogger( getClass ) )

  /** List of keys that are currently being pressed. */
  private val downKeys = mutable.Set[Int]()

  /** Performs the actions when the mouse wheel is moved (not dragged) */
  override def mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    val gameXY = pixelToGameXY(newx, newy)
    val coor = openglToLocation( gameXY._1, gameXY._2 )
    gui.hoverTile = Some( coor )
  }

  /** Performs the actions when the mouse wheel is dragged along */
  override def mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    moveByPx( newx-oldx, newy-oldy )
  }

  /** Performs the actions when the mouse wheel is being moved (scrolled) */
  override def mouseWheelMoved(change: Int) {
    gui.view.scaleBy(change / 500.0f)
  }

  /** Performs the actions when a key is being pressed */
  override def keyPressed(key: Int, c: Char) = {
    downKeys.add(key)
    key match {
      case KEY_UP       => moveByPx( 0,  10)
      case KEY_DOWN     => moveByPx( 0, -10)
      case KEY_LEFT     => moveByPx(  10, 0)
      case KEY_RIGHT    => moveByPx( -10, 0)
      case KEY_ADD      => gui.view.scaleBy( 0.1f)
      case KEY_SUBTRACT => gui.view.scaleBy(-0.1f)
      case KEY_DELETE   =>
        gui.view.scaleTo(0.5f)
        gui.view.moveTo(0, 0)


      case KEY_F4 if isDown(KEY_LALT) || isDown(KEY_RALT) => System.exit(0)
      case _ =>
    }
  }

  /** Performs the actions when a key is being released */
  override def keyReleased(key: Int, c: Char) {
    downKeys.remove(key)
    key match {
      case _ =>
    }
  }

  /** Move the gui viewport by a specific amount pixels */
  def moveByPx(deltaX: Float, deltaY: Float) =
    gui.view.moveBy( deltaX/gui.view.scale, deltaY/gui.view.scale )

  /** Check if a specific key is currently being pressed */
  def isDown(key: Int) = downKeys.contains(key)

  /**
   * Convert an on-screen pixel to an OpenGL (in-game) coordinate.
   *
   * @param x Horizontal pixel index, relative to top-left.
   * @param y Vertical   pixel index, relative to top-left.
   * @return OpenGL coordinate (x, y).
   */
  def pixelToGameXY(x: Int, y: Int) = {
    val offset = gui.view.offset // offset is negative for moving to the right-bottom!
    val xInScreen = x * gui.view.width  / gui.container.getWidth
    val yInScreen = y * gui.view.height / gui.container.getHeight
    ( xInScreen - offset._1, yInScreen - offset._2 )
  }

  /**
   * Convert OpenGL coordinates to the index to a specific tile containing the coordinate.
   *
   * @param x OpenGL x coordinate.
   * @param y OpenGL y coordinate.
   * @return Tile coordinate (u, v)
   */
  def openglToLocation(x: Float, y : Float) =
    HexTile.fromXY( x / GuiTile.scale, y / GuiTile.scale )

}
