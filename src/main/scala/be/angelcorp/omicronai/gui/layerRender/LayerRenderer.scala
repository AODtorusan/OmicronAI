package be.angelcorp.omicronai.gui.layerRender

import org.newdawn.slick.Graphics
import com.lyndir.omicron.api.model.LevelType

trait LayerRenderer {

  def render(g: Graphics, level: LevelType)

  override def toString: String

}
