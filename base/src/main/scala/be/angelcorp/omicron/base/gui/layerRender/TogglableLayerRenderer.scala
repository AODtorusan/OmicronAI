package be.angelcorp.omicron.base.gui.layerRender

import org.newdawn.slick.Graphics
import be.angelcorp.omicron.base.gui.ViewPort
import be.angelcorp.omicron.base.world.SubWorld

class TogglableLayerRenderer( val backend: LayerRenderer, var enabled: Boolean = true ) extends LayerRenderer {

  override def viewChanged(view: ViewPort) =
    backend.viewChanged(view)

  override def prepareRender(subWorld: SubWorld, layer: Int) =
    if (enabled) backend.prepareRender(subWorld, layer)

  override def render(g: Graphics) =
    if (enabled) backend.render(g)

}
