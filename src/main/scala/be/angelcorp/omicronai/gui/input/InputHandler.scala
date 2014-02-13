package be.angelcorp.omicronai.gui.input

import akka.actor.Actor

trait InputHandler extends Actor {

  override def preStart() {
    context.system.eventStream.subscribe(self, classOf[GuiInputEvent])
  }

}
