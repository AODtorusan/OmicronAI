package be.angelcorp.omicron.noai.gui.screens

import akka.actor.Props
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.screen.{Screen, ScreenController}
import de.lessvoid.nifty.Nifty
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicron.noai.gui.NoAiGui

class NoAiUserInterfaceController(val gui: NoAiGui) extends ScreenController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  var nifty: Nifty = null
  gui.noai.actorSystem.actorOf( Props( classOf[NoAiPopupController], this ) )
  gui.noai.actorSystem.actorOf( Props( classOf[NoAiSideBarController], this, gui.controller.guiMessages) )

  override def onStartScreen() {}
  override def onEndScreen() {}

  override def bind(nifty: Nifty, screen: Screen) {
    this.nifty = nifty
  }

}