package be.angelcorp.omicronai.gui.layerRender

import org.newdawn.slick.Graphics
import be.angelcorp.omicronai.gui.ViewPort

class MultiplexRenderer( backend: Iterable[LayerRenderer] ) extends LayerRenderer {

  override def update(view: ViewPort) {
    backend.foreach( b => b.update(view) )
  }

  override def render(g: Graphics, view: ViewPort) {
    backend.foreach( b => b.render(g, view) )
  }

}
