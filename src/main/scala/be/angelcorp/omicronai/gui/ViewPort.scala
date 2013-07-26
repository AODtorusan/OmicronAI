package be.angelcorp.omicronai.gui

import collection.JavaConverters._
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.Location._
import com.lyndir.omicron.api.model.LevelType
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

class ViewPort(gui: AiGui,
               private var _activeLayer: Int       = LevelType.GROUND,
               private var _scale:   Float         =  0.5f,
               private var _offset: (Float, Float) = (0, 0)) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  /** Width  of the in-game viewport (scaled) */
  var width  = 0.0f
  /** Height of the in-game viewport (scaled) */
  var height = 0.0f

  /** Bounds of the in-game screen, with some padding (top-left x, top left y, bottom right x, bottom right y) */
  var screenBounds = (0.0f, 0.0f, 0.0f, 0.0f)
  /** List of all the tiles that can be viewed though the viewport */
  var tilesInView: Iterable[Location] = Nil

  updateScreen

  /** Checks if a given location is currently visible through the viewport */
  def inView( location: Location ) = {
    val center = GuiTile.center( location )

    location.h == _activeLayer &&
      screenBounds._1 < center._1 && center._1 < screenBounds._3 &&
      screenBounds._2 < center._2 && center._2 < screenBounds._4
  }

  /** Update the viewport bounds cache, required after every move/resize */
  private def updateScreen {
    width  = gui.container.getWidth  /_scale
    height = gui.container.getHeight /_scale

    screenBounds = (
      -_offset._1-GuiTile.scale,       -_offset._2-GuiTile.scale,
      -_offset._1+width+GuiTile.scale, -_offset._2+height+GuiTile.scale
    )
    tilesInView = gui.game.getLevel( _activeLayer ).getTiles.values().asScala.map( tile => {
      val loc: Location = tile
      if ( inView(loc) ) Some(loc) else None
    } ).flatten

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

  /** Get the active displayed being displayed in the viewport */
  def activeLayer = _activeLayer

  /** Get the current scale factor of the viewport */
  def scale = _scale

  /** Get the current offset of the viewport in in-game units*/
  def offset = _offset

  override def toString = s"layer=$activeLayer, offset=$offset, scale=$scale, width=$width, height=$height, tiles in view:${tilesInView.size}"

}
