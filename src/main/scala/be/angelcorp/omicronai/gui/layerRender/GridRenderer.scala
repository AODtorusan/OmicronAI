package be.angelcorp.omicronai.gui.layerRender

import collection.JavaConverters._
import com.lyndir.omicron.api.controller.GameController
import be.angelcorp.omicronai.gui.{DrawStyle, GuiTile}
import org.newdawn.slick.{Graphics, Color}
import be.angelcorp.omicronai.PikeAi
import com.lyndir.omicron.api.model.LevelType

class GridRenderer(player: PikeAi, border: DrawStyle = Color.white, fill: Color = Color.black) extends LayerRenderer {

  lazy val game = player.getController.getGameController

  lazy val tiles = for( (coordinate, tile) <- game.listLevels().asScala.head.getTiles.asScala) yield new GuiTile( tile ) {
    val gameTile = tile
    override def fillColor   = fill
    override def borderStyle = border
  }

  def render(g: Graphics, layer: LevelType) {
    try {
      tiles.foreach( _.render(g) )
    } catch {
      case _: Throwable =>
    }
  }

  override def toString: String = "Map grid"

}
