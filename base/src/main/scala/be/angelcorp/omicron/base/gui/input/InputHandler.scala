package be.angelcorp.omicron.base.gui.input

import akka.actor.Actor
import be.angelcorp.omicron.base.util.GenericEventBus

trait InputHandler extends Actor {

  protected def guiEventBus: GenericEventBus

  override def preStart() {
    guiEventBus.subscribe(self, classOf[GuiInputEvent])
  }

}
