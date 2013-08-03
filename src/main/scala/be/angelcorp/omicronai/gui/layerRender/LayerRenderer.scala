package be.angelcorp.omicronai.gui.layerRender

import org.newdawn.slick.Graphics
import be.angelcorp.omicronai.gui.ViewPort

trait LayerRenderer {

  /**
   * Called before the render method when the viewport has changed (position or scale)
   */
  def update(view: ViewPort) { }

  def render(g: Graphics, view: ViewPort)

  override def toString: String

}
