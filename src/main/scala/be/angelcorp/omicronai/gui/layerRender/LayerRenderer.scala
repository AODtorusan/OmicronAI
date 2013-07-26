package be.angelcorp.omicronai.gui.layerRender

import org.newdawn.slick.Graphics
import be.angelcorp.omicronai.gui.ViewPort

trait LayerRenderer {

  def render(g: Graphics, view: ViewPort)

  override def toString: String

}
