package be.angelcorp.omicronai.ai.noai.gui.screens

import org.slf4j.LoggerFactory
import de.lessvoid.nifty.screen.{Screen, ScreenController}
import de.lessvoid.nifty.{NiftyEventAnnotationProcessor, Nifty}
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.LevelType
import be.angelcorp.omicronai.ai.noai.gui.NoAiGui

class NoAiUserInterfaceController(val gui: NoAiGui) extends ScreenController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  var nifty: Nifty = null
  val sidebarController = new NoAiSideBarController(this)
  val popupController   = new NoAiPopupController(this)
  gui.noai.actorSystem.actorOf( popupController.inputHandler )

  override def onStartScreen() {}
  override def onEndScreen() {}

  override def bind(nifty: Nifty, screen: Screen) {
    this.nifty = nifty

    NiftyEventAnnotationProcessor.process( sidebarController )
    NiftyEventAnnotationProcessor.process( popupController   )
    sidebarController.populate()
    popupController.populate()
  }

}