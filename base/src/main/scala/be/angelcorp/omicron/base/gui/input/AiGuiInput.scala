package be.angelcorp.omicron.base.gui.input

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import org.newdawn.slick.Input._
import be.angelcorp.omicron.base.configuration.Configuration.config
import be.angelcorp.omicron.base.gui.AiGuiOverlay

class AiGuiInput(gui: AiGuiOverlay) extends InputHandler {
  private val logger = Logger( LoggerFactory.getLogger( getClass ) )

  override def receive = {
    case MouseMoved(_, _, x, y)               => updateTileHover(x, y)
    case MouseDragged(fromX, fromY, toX, toY, 0) => moveByPx( toX-fromX, toY-fromY)
    case MouseWheelMoved(_,_,delta)           => gui.view.scaleBy(delta / 500.0f)

    case m: GuiInputEvent if config.gui.cameraNorth(m)     => moveByPx(   0,   10)
    case m: GuiInputEvent if config.gui.cameraNorthFast(m) => moveByPx(   0,  100)
    case m: GuiInputEvent if config.gui.cameraEast(m)      => moveByPx( -10,    0)
    case m: GuiInputEvent if config.gui.cameraEastFast(m)  => moveByPx(-100,    0)
    case m: GuiInputEvent if config.gui.cameraSouth(m)     => moveByPx(   0,  -10)
    case m: GuiInputEvent if config.gui.cameraSouthFast(m) => moveByPx(   0, -100)
    case m: GuiInputEvent if config.gui.cameraWest(m)      => moveByPx(  10,    0)
    case m: GuiInputEvent if config.gui.cameraWestFast(m)  => moveByPx( 100,    0)

    case m: GuiInputEvent if config.gui.cameraIn(m)        => gui.view.scaleBy(  0.1f )
    case m: GuiInputEvent if config.gui.cameraInFast(m)    => gui.view.scaleBy(  1.0f )
    case m: GuiInputEvent if config.gui.cameraOut(m)       => gui.view.scaleBy( -0.1f )
    case m: GuiInputEvent if config.gui.cameraOutFast(m)   => gui.view.scaleBy( -1.0f )
    case m: GuiInputEvent if config.gui.cameraReset(m)     => gui.view.scaleTo(1f); gui.view.moveTo(0, 0)

    case KeyPressed(KEY_F4, _, _, true)=> System.exit(0)
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
