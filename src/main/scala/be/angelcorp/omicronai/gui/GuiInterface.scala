package be.angelcorp.omicronai.gui

import scala.collection.mutable
import de.lessvoid.nifty.Nifty
import be.angelcorp.omicronai.gui.layerRender.LayerRenderer

trait GuiInterface {

  val activeLayers = mutable.ListBuffer[ LayerRenderer ]()

}
