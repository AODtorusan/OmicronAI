package be.angelcorp.omicronai.gui.layerRender

import org.newdawn.slick.Graphics
import be.angelcorp.omicronai.gui.ViewPort
import be.angelcorp.omicronai.world.SubWorld

class MultiplexRenderer( backend: Iterable[LayerRenderer] ) extends LayerRenderer {

  override def viewChanged(view: ViewPort) {
    backend.foreach( b => b.viewChanged(view) )
  }

  override def prepareRender(subWorld: SubWorld, layer: Int) {
    backend.foreach( b => b.prepareRender(subWorld, layer) )
  }

  override def render(g: Graphics) {
    backend.foreach( b => b.render(g) )
  }

}
