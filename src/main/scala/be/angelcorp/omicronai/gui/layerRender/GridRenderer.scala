package be.angelcorp.omicronai.gui.layerRender

import be.angelcorp.omicronai.gui.{ViewPort, DrawStyle, GuiTile}
import org.newdawn.slick.{Graphics, Color}
import be.angelcorp.omicronai.{HexTile, PikeAi}
import org.newdawn.slick.geom.Polygon

class GridRenderer(player: PikeAi, border: DrawStyle = Color.white) extends LayerRenderer {

  lazy val game  = player.getController.getGameController
  lazy val xsize = game.listLevels().iterator().next().getSize.getWidth
  lazy val ysize = game.listLevels().iterator().next().getSize.getHeight

  val horizontal = for( u <- 0 to xsize  ) yield {
     val poly = new Polygon(
       (for( v <- 0 until ysize ) yield {
         val center = HexTile(u, v).centerXY
         Seq( center._1 - 0.50f * HexTile.width, center._2 - 0.25f * HexTile.height,
              center._1 - 0.50f * HexTile.width, center._2 + 0.25f * HexTile.height,
              center._1 + 0.00f * HexTile.width, center._2 + 0.50f * HexTile.height )
       }).flatten.map( _ * GuiTile.scale ).toArray )
     poly.setClosed(false)
     poly
  }

  val vertical = for( v <- 0 to ysize  ) yield {
    val poly = new Polygon(
      (for( u <- 0 until xsize ) yield {
        val center = HexTile(u, v).centerXY
        Seq( center._1 - 0.50f * HexTile.width, center._2 - 0.25f * HexTile.height,
             center._1 + 0.00f * HexTile.width, center._2 - 0.50f * HexTile.height,
             center._1 + 0.50f * HexTile.width, center._2 - 0.25f * HexTile.height )
      }).flatten.map( _ * GuiTile.scale ).toArray )
    poly.setClosed(false)
    poly
  }

  def render(g: Graphics, view: ViewPort) {
    border.applyOnto(g)
    horizontal.foreach( g.draw )
    vertical.foreach( g.draw )
  }

  override def toString: String = "Map grid"

}
