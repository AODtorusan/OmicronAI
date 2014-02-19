package be.angelcorp.omicron.base.gui

import be.angelcorp.omicron.base.gui.layerRender.LayerRenderer

trait GuiInterface {

  def activeLayers: Seq[ LayerRenderer ]

}
