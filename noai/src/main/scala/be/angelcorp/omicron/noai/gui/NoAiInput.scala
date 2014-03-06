package be.angelcorp.omicron.noai.gui

import scala.collection.JavaConverters._
import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.base.ai.actions.MoveAction
import be.angelcorp.omicron.base.configuration.Configuration.config
import be.angelcorp.omicron.base.gui.input.{GuiInputEvent, MouseClicked, InputHandler}
import be.angelcorp.omicron.noai.NoAi

class NoAiInput(noai: NoAi, gui: NoAiGui) extends InputHandler {

  implicit val game = noai.getController.getGameController.getGame

  override def receive = {
    case MouseClicked(x, y, 0, 1) =>
      Location(gui.frame.view.pixelToTile(x, y), gui.frame.view.activeLayer, noai.gameSize).reduce.map {
        case selectedLocation =>
          noai.unitOn( selectedLocation ) match {
            case Some( asset ) => noai.select( asset )
            case None =>
              noai.plannedAction match {
                case Some(mv: MoveAction) if mv.destination == selectedLocation => noai.updateOrConfirmAction(mv)
                case _ => noai.selected match {
                  case Some(asset) => noai.updateOrConfirmAction( new MoveAction(asset, selectedLocation, noai.world) )
                  case _ =>
                }
              }
          }
      }


    case m: GuiInputEvent if config.noai.updateOrConfirmAction(m) =>
      noai.plannedAction.map( a => noai.updateOrConfirmAction( a ) )

    case m: GuiInputEvent if config.noai.endTurn(m) =>
      noai.endTurn()

    case m: GuiInputEvent if config.noai.nextUnit(m) =>
      noai.selectNext()
      noai.selected.map( gui.moveTo )

    case m: GuiInputEvent if config.noai.previousUnit(m) =>
      noai.selectPrevious()
      noai.selected.map( gui.moveTo )

    case m: GuiInputEvent if config.noai.centerView(m) =>
      val units = noai.units
      val sum = units.foldLeft( (0.0,0.0) )( (loc, unit) => {
        val l = unit.location.get
        (loc._1 + l.u, loc._2 + l.v)
      } )
      val u = sum._1 / units.size
      val v = sum._2 / units.size
      gui.frame.view.centerOn( u.toInt, v.toInt )
  }

}
