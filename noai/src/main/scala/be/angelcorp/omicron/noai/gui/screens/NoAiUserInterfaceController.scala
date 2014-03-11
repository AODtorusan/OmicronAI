package be.angelcorp.omicron.noai.gui.screens

import org.slf4j.LoggerFactory
import de.lessvoid.nifty.screen.{Screen, ScreenController}
import de.lessvoid.nifty.{NiftyEventAnnotationProcessor, Nifty}
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicron.noai.gui.{SelectionChanged, NoAiGui}
import akka.actor.{Actor, Props}
import be.angelcorp.omicron.base.bridge.GameListenerMessage
import be.angelcorp.omicron.noai.gui.screens.NoAiSideBarController

class NoAiUserInterfaceController(val gui: NoAiGui) extends ScreenController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  var nifty: Nifty = null
  val popupController   = new NoAiPopupController(this)
  gui.noai.actorSystem.actorOf( popupController.inputHandler )
  gui.noai.actorSystem.actorOf( Props( classOf[NoAiSideBarController], this, gui.controller.guiMessages) )

  override def onStartScreen() {}
  override def onEndScreen() {}

  override def bind(nifty: Nifty, screen: Screen) {
    this.nifty = nifty

    NiftyEventAnnotationProcessor.process( popupController   )
    popupController.populate()
  }

}