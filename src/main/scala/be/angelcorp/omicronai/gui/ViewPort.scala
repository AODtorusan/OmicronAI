package be.angelcorp.omicronai.gui

import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.LevelType
import be.angelcorp.omicronai.{HexTile, Location}
import be.angelcorp.omicronai.Location._
import be.angelcorp.omicronai.world.WorldBounds

class ViewPort(gui: AiGuiOverlay,
               private var _activeLayer: Int       = LevelType.GROUND,
               private var _scale:   Float         = 1f,
               private var _offset: (Float, Float) = (0, 0),
               private var _changed: Boolean       = true) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  /** Width  of the in-game viewport (scaled) */
  var width  = 0.0f
  /** Height of the in-game viewport (scaled) */
  var height = 0.0f

  /** Bounds of the in-game screen, with some padding (top-left x, top left y, bottom right x, bottom right y) */
  var screenBounds = (0.0f, 0.0f, 0.0f, 0.0f)
  /** List of all the tiles that can be viewed though the viewport */
  var tilesInView: Iterable[Location] = Nil
  /** World bounds of the current view */
  var viewBounds = WorldBounds(0,0,0,0,0,0)

  updateScreen

  /** Checks if a given location is currently visible through the viewport */
  def inView( location: Location ) = {
    val center = Canvas.center( location )

    location.h == _activeLayer &&
      screenBounds._1 < center._1 && center._1 < screenBounds._3 &&
      screenBounds._2 < center._2 && center._2 < screenBounds._4
  }

  /** Update the viewport bounds cache, required after every move/resize */
  private def updateScreen {
    width  = gui.container.getWidth  /_scale
    height = gui.container.getHeight /_scale

    screenBounds = (
      -_offset._1-Canvas.scale,       -_offset._2-Canvas.scale,
      -_offset._1+width+Canvas.scale, -_offset._2+height+Canvas.scale
    )
    tilesInView = gui.game.getLevel( _activeLayer ).getTiles.values().asScala.map( tile => {
      val loc: Location = tile
      if ( inView(loc) ) Some(loc) else None
    } ).flatten

    var minU = Int.MaxValue
    var maxU = Int.MinValue
    var minV = Int.MaxValue
    var maxV = Int.MinValue
    for (tile <- tilesInView) {
      if (tile.u < minU) minU = tile.u
      if (tile.u > maxU) maxU = tile.u
      if (tile.v < minV) minV = tile.v
      if (tile.v > maxV) maxV = tile.v
    }
    if (minU > maxU) {minU = 0; maxU = 0} // World out of view!
    if (minV > maxV) {minV = 0; maxV = 0} // World out of view!
    viewBounds = WorldBounds(maxU - minU + 1, maxV - minV + 1, u0 = minU, v0 = minV )

    _changed = true

    logger.debug(s"Viewport changed to; $toString")
  }
  /** Zoom the viewport in/out of the current game */
  def scaleBy(delta: Float) {
    scaleTo( _scale + delta )
  }

  /** Set the zoomlevel of the viewport to a known level */
  def scaleTo(newScale: Float) {
    if (newScale > 0) {
      val dx = gui.container.getWidth  * ( 1.0f/newScale - 1.0f/_scale)
      val dy = gui.container.getHeight * ( 1.0f/newScale - 1.0f/_scale)

      _scale = newScale
      moveBy(dx/2.0f, dy/2.0f)
    } else {
      logger.warn(s"Cannot rescale the viewport to a scale <= 0, requested $newScale")
    }
  }

  /** Move the viewport upper left corner by a predefined amount of in-game units */
  def moveBy( deltaX: Float, deltaY: Float) {
    _offset = (_offset._1 + deltaX, _offset._2 + deltaY)
    updateScreen
  }

  /** Move the viewport upper left corner to a predefined location of in-game units */
  def moveTo( xOffset: Float, yOffset: Float) {
    _offset = (xOffset, yOffset)
    updateScreen
  }

  /** Change the active displayed being displayed in the viewport */
  def activeLayer_=(layer: Int) {
    _activeLayer = layer
    updateScreen
  }

  /** Change the active displayed being displayed in the viewport */
  def activeLayer_=(layer: LevelType) {
    _activeLayer = layer
    updateScreen
  }

  /** Move the viewport so that a specific tile is in the center of the view */
  def centerOn(u: Int, v: Int) {
    val center = Canvas.center(u, v)
    val newX = - center._1 + width  / 2.0f
    val newY = - center._2 + height / 2.0f
    moveTo( newX, newY )
  }

  /** Move the viewport so that a specific tile is in the center of the view */
  def centerOn(l: Location) { centerOn( l.u, l.v ) }

  /** Get the active displayed being displayed in the viewport */
  def activeLayer = _activeLayer

  /** Get the current scale factor of the viewport */
  def scale = _scale

  /** Get the current offset of the viewport in in-game units*/
  def offset = _offset

  /** Check if the viewport has change since the last poll */
  def changed = _changed

  protected[gui] def unsetChanged() {
    _changed = false
  }

  override def toString = f"h=$activeLayer p=(${offset._1}%.0f, ${offset._2}%.0f) s=$scale%.2f w=$width%.0f h=$height%.0f tiles=${tilesInView.size}"

  /**
   * Convert an on-screen pixel to an OpenGL (in-game) coordinate.
   *
   * @param x Horizontal pixel index, relative to top-left.
   * @param y Vertical   pixel index, relative to top-left.
   * @return OpenGL coordinate (x, y).
   */
  def pixelToOpengl(x: Int, y: Int) = {
    val xInScreen = x * width  / gui.container.getWidth
    val yInScreen = y * height / gui.container.getHeight
    ( xInScreen - offset._1, yInScreen - offset._2 )
  }

  /**
   * Convert OpenGL coordinates to the index to a specific tile containing the coordinate.
   *
   * @param x OpenGL x coordinate.
   * @param y OpenGL y coordinate.
   * @return Tile coordinate (u, v)
   */
  def openglToTile(x: Float, y : Float) =
    HexTile.fromXY( x / Canvas.scale, y / Canvas.scale )

  def pixelToTile( x: Int, y: Int ) = {
    val (oglx, ogly) = pixelToOpengl(x, y)
    openglToTile(oglx, ogly)
  }

}
