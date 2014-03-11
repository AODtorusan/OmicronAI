package be.angelcorp.omicron.noai.gui

import scala.collection.mutable
import de.lessvoid.nifty.Nifty
import be.angelcorp.omicron.base.ai.actions.Action
import be.angelcorp.omicron.base.bridge.Asset
import be.angelcorp.omicron.base.gui.{AiGuiOverlay, GuiInterface}
import be.angelcorp.omicron.base.util.GenericEventBus
import be.angelcorp.omicron.noai.NoAi

class GuiController(val noai: NoAi, val frame: AiGuiOverlay, val nifty: Nifty) extends GuiInterface {

  val guiMessages = new GenericEventBus

  protected[noai] val _plannedActions = mutable.Map[Asset, Option[Action]]()
  protected[noai] def plannedAction = (selected flatMap _plannedActions.get).flatten

  protected[noai] var _selected: Option[Asset] = None
  protected[noai] def selected = _selected

  val gui = new NoAiGui( this )

  override def activeLayers = gui.activeLayers

  protected[noai] def select( asset: Asset): Unit = {
    val from = _selected
    _selected = Some(asset)
    guiMessages.publish( SelectionChanged( from, _selected ) )
  }

  protected[noai] def selectNext(): Unit = {
    _selected match {
      case Some(unit) =>
        val idx = (noai.units.indexOf( unit ) + 1) % noai.units.size
        select( noai.units(idx) )
      case None =>
        noai.units.headOption.map( select )
    }
  }

  protected[noai] def selectPrevious(): Unit = {
    _selected match {
      case Some(unit) =>
        val idx = (noai.units.indexOf( unit ) - 1 + noai.units.size) % noai.units.size
        select( noai.units(idx) )
      case None =>
        noai.units.headOption.map( select )
    }
  }

  protected[noai] def updateOrConfirmAction( action: Action) =
    plannedAction match {
      // Execute the plan (the same plan was passed in)
      case Some(plan) if plan == action =>
        noai.execute( plan )
      // Update the plan
      case _ =>
        _plannedActions.update( selected.get, Some(action))
    }

}

case class SelectionChanged( from: Option[Asset], to: Option[Asset] )
case class LevelChanged( from: Int, to: Int )
