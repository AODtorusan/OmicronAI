package be.angelcorp.omicronai.ai.noai.gui

import scala.collection.JavaConverters._
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.ai.actions.MoveAction
import be.angelcorp.omicronai.ai.noai.NoAi
import be.angelcorp.omicronai.configuration.Configuration.config
import be.angelcorp.omicronai.gui.input._

class NoAiInput(noai: NoAi, gui: NoAiGui) extends InputHandler {

  override def receive = {
    case MouseClicked(x, y, 0, 1) =>
      val selectedLocation = Location(gui.frame.view.pixelToTile(x, y), gui.frame.view.activeLayer, noai.gameSize).reduce
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

    case m: GuiInputEvent if config.noai.updateOrConfirmAction(m) =>
      noai.plannedAction.map( a => noai.updateOrConfirmAction( a ) )

    case m: GuiInputEvent if config.noai.endTurn(m) =>
      noai.endTurn()

    case m: GuiInputEvent if config.noai.nextUnit(m) =>      // TODO
    case m: GuiInputEvent if config.noai.previousUnit(m) =>  // TODO

    case m: GuiInputEvent if config.noai.centerView(m) =>
      noai.withSecurity(noai.getKey) {
        val objs = noai.getController.listObjects()
        val sum = objs.asScala.foldLeft( (0.0,0.0) )( (loc, unit) => {
          val l = unit.checkLocation().get().getPosition
          (loc._1 + l.getU, loc._2 + l.getV)
        } )
        val u = sum._1 / objs.size()
        val v = sum._2 / objs.size()
        gui.frame.view.centerOn( u.toInt, v.toInt )
      }
  }

}
