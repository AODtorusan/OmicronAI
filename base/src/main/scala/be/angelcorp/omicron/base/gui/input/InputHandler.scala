package be.angelcorp.omicron.base.gui.input

import akka.actor.Actor

trait InputHandler extends Actor {

  override def preStart() {
    context.system.eventStream.subscribe(self, classOf[GuiInputEvent])
  }

}
