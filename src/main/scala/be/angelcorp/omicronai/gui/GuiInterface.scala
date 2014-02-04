package be.angelcorp.omicronai.gui

import be.angelcorp.omicronai.gui.layerRender.LayerRenderer

trait GuiInterface {

  def activeLayers: Seq[ LayerRenderer ]

}
