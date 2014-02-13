package be.angelcorp.omicronai.gui.input

import akka.actor.Actor
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Keyboard._

trait InputHandler extends Actor {

  override def preStart() {
    context.system.eventStream.subscribe(self, classOf[GuiInputEvent])
  }

  def isKeyDown( key: Int ) = Keyboard.isKeyDown( key )
  def isAltDown = isKeyDown( KEY_LMENU ) || isKeyDown( KEY_RMENU )

}
