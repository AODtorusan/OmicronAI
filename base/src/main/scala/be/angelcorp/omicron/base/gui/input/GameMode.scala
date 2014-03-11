package be.angelcorp.omicron.base.gui.input

import org.newdawn.slick.{GameContainer, Input}
import de.lessvoid.nifty.slick2d.NiftyOverlayGameState
import org.newdawn.slick.state.StateBasedGame

trait GameMode extends NiftyOverlayGameState {

  def id: Int
  protected var renderPaused = false
  protected var updatePaused = false
  
  override def keyReleased(key: Int, c: Char) {}
  override def keyPressed(key: Int, c: Char) {}
  override def controllerButtonReleased(controller: Int, button: Int) {}
  override def controllerButtonPressed(controller: Int, button: Int) {}
  override def controllerDownReleased(controller: Int) {}
  override def controllerDownPressed(controller: Int) {}
  override def controllerUpReleased(controller: Int) {}
  override def controllerUpPressed(controller: Int) {}
  override def controllerRightReleased(controller: Int) {}
  override def controllerRightPressed(controller: Int) {}
  override def controllerLeftReleased(controller: Int) {}
  override def controllerLeftPressed(controller: Int) {}
  override def inputStarted() {}
  override def inputEnded() {}
  override def isAcceptingInput = false
  override def setInput(input: Input) {}
  override def mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {}
  override def mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {}
  override def mouseReleased(button: Int, x: Int, y: Int) {}
  override def mousePressed(button: Int, x: Int, y: Int) {}
  override def mouseClicked(button: Int, x: Int, y: Int, clickCount: Int) {}
  override def mouseWheelMoved(change: Int) {}

  override def leaveState(container: GameContainer, game: StateBasedGame) {}
  override def enterState(container: GameContainer, game: StateBasedGame) {}

  override def getID = id

  override def setRenderPaused(pause: Boolean): Unit = renderPaused = pause
  override def setUpdatePaused(pause: Boolean): Unit = updatePaused = pause
  override def isRenderPaused = renderPaused
  override def isUpdatePaused = updatePaused
  override def unpauseRender() { renderPaused = false }
  override def unpauseUpdate() { updatePaused = false }
  override def pauseRender()   { renderPaused = true  }
  override def pauseUpdate()   { updatePaused = true  }
}
