package be.angelcorp.omicron.base.gui

import be.angelcorp.omicron.base.gui.layerRender.LayerRenderer

trait GuiInterface {

  private def activeLayers: Seq[ LayerRenderer ] = Nil

}
