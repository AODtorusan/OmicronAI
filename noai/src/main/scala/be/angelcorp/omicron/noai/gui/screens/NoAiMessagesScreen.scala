package be.angelcorp.omicron.noai.gui.screens

import scala.collection.JavaConverters._
import akka.actor.{Actor, Props}
import de.lessvoid.nifty.screen.{Screen, ScreenController}
import de.lessvoid.nifty.{NiftyEvent, NiftyEventSubscriber, Nifty}
import de.lessvoid.nifty.controls.{ButtonClickedEvent, ListBox}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicron.base.gui.{ScreenFill, GuiScreen}
import be.angelcorp.omicron.base.gui.nifty.NiftyConstants._
import be.angelcorp.omicron.noai.GuiMessage
import be.angelcorp.omicron.noai.gui.{SelectionChanged, NoAiGui}

object NoAiMessagesScreen extends GuiScreen {
  override val screenId   = "messagesScreen"
  override val screenType = ScreenFill

  def screen(noaiGui: NoAiGui) = {
    val xml =
    //<?xml version="1.0" encoding="UTF-8"?>
      <nifty xmlns="http://nifty-gui.lessvoid.com/nifty-gui" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" >
        <useControls filename="nifty-default-controls.xml"/>
        <screen id={screenId} controller={classOf[NoAiMessagesScreenController].getName}>
          <layer id="contentLayer" childLayout="vertical" backgroundColor={black}>
            <panel id="messagesPanel" childLayout="vertical" width="*" height="*" padding="10px">
              <control id="messagesList" name="listBox" displayItems="21" vertical="on" horizontal="optional" width="*" height="*" selection="Single" />
            </panel>
            <panel id="bottom" childLayout="horizontal" width="*" height="40px" padding="10px">
              <control name="label" text="" width="10%"/>
              <control id="back"   name="button" label="Back" height="*" width="30%" />
              <control name="label" text="" width="20%"/>
              <control id="activate" name="button" label="Activate" height="*" width="30%" />
              <control name="label" text="" width="10%"/>
            </panel>
          </layer>
        </screen>
      </nifty>;

    loadNiftyXml( noaiGui.nifty, xml, new NoAiMessagesScreenController(noaiGui) )
    noaiGui.nifty.getScreen( screenId )
  }
}

class NoAiMessagesScreenController(val gui: NoAiGui) extends ScreenController {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  lazy val nifty = gui.nifty

  lazy val messagesScreen = nifty.getScreen(NoAiMessagesScreen.screenId)
  lazy val messagesList   = messagesScreen.findNiftyControl("messagesList", classOf[ListBox[GuiMessage]])

  val listener = gui.noai.actorSystem.actorOf( Props( new Actor {
    override def preStart(): Unit =
      gui.controller.guiMessages.subscribe(context.self, classOf[AnyRef])
    override def receive = {
      case m: GuiMessage => messagesList.addItem( m )
    }
  } ) )


  override def bind(nifty: Nifty, screen: Screen) {}
  override def onEndScreen() {}
  override def onStartScreen() {}

  @NiftyEventSubscriber(id = "back")
  def backButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      gui.gotoScreen( NoAiUserInterface )
    case _ =>
  }

  @NiftyEventSubscriber(id = "activate")
  def buildButtonAction(id: String, event: NiftyEvent) = event match {
    case e: ButtonClickedEvent =>
      messagesList.getSelection.asScala.headOption match {
        case Some( selected ) => selected.onClick( gui )
        case None => logger.info("Clicked on activate message, but no message was selected.")
      }
    case _ =>
  }

}