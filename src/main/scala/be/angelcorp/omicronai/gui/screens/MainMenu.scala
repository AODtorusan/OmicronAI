package be.angelcorp.omicronai.gui.screens

import collection.JavaConverters._
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import akka.actor.ActorRef
import akka.util.Timeout
import scala.concurrent.Await
import de.lessvoid.nifty.{NiftyEventSubscriber, Nifty}
import de.lessvoid.nifty.screen.{ScreenController, Screen}
import de.lessvoid.nifty.builder.{PanelBuilder, LayerBuilder, ScreenBuilder}
import de.lessvoid.nifty.controls.button.builder.ButtonBuilder
import de.lessvoid.nifty.controls.listbox.builder.ListBoxBuilder
import de.lessvoid.nifty.controls.{ListBoxSelectionChangedEvent, ListBox}
import org.slf4j.LoggerFactory
import org.newdawn.slick.{Graphics, Color}
import com.typesafe.scalalogging.slf4j.Logger
import com.lyndir.omicron.api.model.{LevelType, Player}
import be.angelcorp.omicronai.gui.NiftyConstants._
import be.angelcorp.omicronai.gui.effects.MoveEffectBuilder
import be.angelcorp.omicronai.gui.layerRender._
import be.angelcorp.omicronai.gui._
import be.angelcorp.omicronai.agents._
import be.angelcorp.omicronai.actions._
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.PikeAi
import scala.Some
import be.angelcorp.omicronai.agents.ValidateAction
import be.angelcorp.omicronai.agents.Name
import be.angelcorp.omicronai.agents.GetAsset
import org.newdawn.slick.geom.Polygon

object MainMenu extends GuiScreen {

  val name = "mainMenuScreen"

  def screen(nifty: Nifty, gui: AiGui) = {
    new ScreenBuilder( name ) {{

      controller(new MainMenuController(gui))

      layer(new LayerBuilder("content") {{
        backgroundColor( transparent )
        childLayoutVertical()

        panel(new PanelBuilder("middle") {{
          backgroundColor( black(128) )
          childLayoutCenter()
          width(percentage(25))
          alignRight()
          valignTop()
          height("*")
          visibleToMouse()
          paddingRight(pixels(5))

          onStartScreenEffect(new MoveEffectBuilder() {{
            mode( inMode )
            direction( directionRight )
            length(500)
            inherit(true)
          }})

          onEndScreenEffect(new MoveEffectBuilder() {{
            mode( outMode )
            direction( directionRight )
            length(500)
            inherit(true)
          }})

          panel(new PanelBuilder("menu-main") {{

            childLayoutVertical()
            alignCenter()
            valignCenter()

            width(percentage(100))

            control( new ListBoxBuilder("activeLayerList") {{
              width(percentage(100))
              marginBottom(pixels(5))

              displayItems(3)
              selectionModeSingle()

              hideHorizontalScrollbar()
              hideVerticalScrollbar()
            }} )

            control( new ListBoxBuilder("layerList") {{
              width(percentage(100))
              marginBottom(pixels(5))

              displayItems(4)
              hideHorizontalScrollbar()
              selectionModeMutliple()
            }} )


            control( new ListBoxBuilder("actionList") {{
              width(percentage(100))
              marginBottom(pixels(5))

              alignCenter()
              valignCenter()

              displayItems(4)
              hideHorizontalScrollbar()
              optionalVerticalScrollbar()
              selectionModeSingle()
            }} )

            panel( new PanelBuilder("actionButtons") {{
              marginTop(pixels(10))

              childLayoutHorizontal()
              height(pixels(20))
              alignCenter()
              valignCenter()
              marginBottom(pixels(5))

              control( new ButtonBuilder("acceptActionButton", "Accept") {{
                alignCenter()
                valignCenter()
                interactOnClick("acceptPressed()")
              }} )
              control( new ButtonBuilder("rejectActionButton", "Reject") {{
                alignCenter()
                valignCenter()
                marginLeft(pixels(10))
                interactOnClick("rejectPressed()")
              }} )
            }} )

            control( new ButtonBuilder("playButton", "Play") {{
              marginTop(pixels(10))
              alignCenter()
              valignCenter()

              interactOnClick("play()")
            }} )

            control(new ButtonBuilder("optionsButton", "Options") {{
              width(pixels(100))

              alignCenter()
              valignCenter()

              interactOnClick("options()")
            }})

            control(new ButtonBuilder("highscoresButton", "Highscores") {{
              width(pixels(100))

              alignCenter()
              valignCenter()

              interactOnClick("highscores()")
            }})

            control(new ButtonBuilder("creditsButton", "Credits") {{
              width(pixels(100))

              alignCenter()
              valignCenter()

              interactOnClick("credits()")
            }})

            control(new ButtonBuilder("exitButton", "Exit") {{
              width(pixels(100))

              alignCenter()
              valignCenter()

              interactOnClick("exit()")
            }})

          }})

        }})

      }})
    }}.build(nifty)
  }

}

class MainMenuController(gui: AiGui) extends ScreenController with GuiSupervisorInterface {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  implicit val timeout: Timeout = 5 seconds

  var nifty: Nifty = null
  val supervisor = gui.supervisor
  supervisor.listener = Some(this)

  lazy val actionList = nifty.getScreen(MainMenu.name).findNiftyControl("actionList", classOf[ListBox[WrappedAction]])

  //implicit val system = pike.actorSystem
  //val validatorRef    = system.actorOf( Props( classOf[ActionValidator], this ), name = "GuiActionValidator" )
  //val validator       = Await.result(validatorRef ? Self(), timeout.duration).asInstanceOf[ActionValidator]

  override def onStartScreen() {}
  override def onEndScreen() {}

  override def bind(nifty: Nifty, screen: Screen) {
    this.nifty = nifty
  }

  def updateLayers(event: ListBoxSelectionChangedEvent[LayerRenderer]) {
    try{
      event.getListBox.getItems.asScala.zipWithIndex.foreach( entry => {
        val renderer = entry._1
        val index    = entry._2
        if ( event.getSelectionIndices.contains(index) ) {
          if (!gui.renderLayers.contains(renderer)) {
            logger.info("Enabling extra layer info: " + renderer.toString )
            gui.renderLayers.append( renderer )
          } else {
            logger.debug("Selected layer info " + renderer.toString + " was already enabled.")
          }
        } else {
          if (gui.renderLayers.contains(renderer)) {
            logger.info("Disabling extra layer info: " + renderer.toString )
            gui.renderLayers.remove( gui.renderLayers.indexOf(renderer) )
          } else {
            logger.trace("Deselected layer info " + renderer.toString + " was already disabled.")
          }
        }
      } )
    } catch {
      case e: Throwable => logger.info("Could not swap display layer, could not retrieve layer details; ", e)
    }
  }

  @NiftyEventSubscriber(pattern=".*List")
  def onListBoxSelectionChanged(id: String, event: ListBoxSelectionChangedEvent[_]) {
    id match {
      case "layerList"       => updateLayers( event.asInstanceOf[ListBoxSelectionChangedEvent[LayerRenderer]] )
      case "actionList"      =>
      case "activeLayerList" => updateActiveLayer( event.asInstanceOf[ListBoxSelectionChangedEvent[LevelType]] )
    }
  }

  def updateActiveLayer( event: ListBoxSelectionChangedEvent[LevelType] ) {
    event.getSelection.asScala.headOption match {
      case Some(newLayer) => gui.activeLayer = newLayer
      case _ =>
    }
  }

  def selectedAction = actionList.getSelection.asScala.headOption

  def acceptPressed() {
    selectedAction match {
      case Some(wrappedAction) =>
        actionList.removeItem(wrappedAction)
        supervisor.acceptAction( wrappedAction )
      case None =>
    }
  }

  def rejectPressed() {
    selectedAction match {
      case Some(wrappedAction) =>
        actionList.removeItem(wrappedAction)
        supervisor.rejectAction( wrappedAction )
      case None =>
    }
  }

  def play() {}
  def options() {}
  def highscores() {}

  def credits() {
    nifty.gotoScreen( Credits.name )
  }

  def exit() {
    nifty.exit()
    System.exit(0)
  }

  def actionReceived(wrappedAction: WrappedAction) {
    actionList.addItem(wrappedAction)
  }

  def newTurn() {}

}
