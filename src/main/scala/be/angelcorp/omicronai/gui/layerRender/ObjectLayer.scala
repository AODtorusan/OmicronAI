package be.angelcorp.omicronai.gui.layerRender

import collection.JavaConverters._
import com.lyndir.omicron.api.model.{GameObject, LevelType, Player}
import org.newdawn.slick.{Graphics, Color}
import be.angelcorp.omicronai.gui.{ViewPort, DrawStyle, GuiTile}
import be.angelcorp.omicronai.Conversions._
import be.angelcorp.omicronai.Present

class ObjectLayer(player: Player,
                  filter: GameObject=>Boolean,
                  name:   String,
                  fill:   Color     = Color.green,
                  border: DrawStyle = Color.transparent) extends LayerRenderer {

  def render(g: Graphics, view: ViewPort) {
    player.getController.listObservableTiles().iterator().asScala.map( tile => toMaybe(tile.checkContents()) match {
      case Present( go ) if view.inView(go.getLocation) && filter(go) => Some(go)
      case _ => None
    } ).flatten.foreach( go => {
      new GuiTile( go.getLocation ) {
        override def borderStyle: DrawStyle = border
        override def fillColor: Color       = fill
      }.render(g)
    } )
  }

  override def toString = name
}
