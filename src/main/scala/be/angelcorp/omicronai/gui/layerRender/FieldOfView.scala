package be.angelcorp.omicronai.gui.layerRender

import collection.JavaConverters._
import org.newdawn.slick.{Graphics, Color}
import com.lyndir.omicron.api.model.Player
import be.angelcorp.omicronai.gui.{ViewPort, DrawStyle, GuiTile}

class FieldOfView(player: Player, fill: Color = Color.white) extends LayerRenderer {

  def render(g: Graphics, view: ViewPort) {
    player.listObservableTiles().iterator().asScala.filter( view.inView(_) ).foreach( tile =>
      new GuiTile( tile ) {
        override def borderStyle: DrawStyle = Color.transparent
        override def fillColor: Color       = fill
      }.render(g)
    )
  }

  override def toString: String = "Field of view"
}
