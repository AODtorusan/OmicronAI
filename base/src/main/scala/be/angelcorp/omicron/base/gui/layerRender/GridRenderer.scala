package be.angelcorp.omicron.base.gui.layerRender

import org.newdawn.slick.{Graphics, Color}
import org.newdawn.slick.geom.Polygon
import be.angelcorp.omicron.base.HexTile
import be.angelcorp.omicron.base.ai.AI
import be.angelcorp.omicron.base.gui.Canvas
import be.angelcorp.omicron.base.gui.slick.DrawStyle
import be.angelcorp.omicron.base.world.SubWorld

class GridRenderer(player: AI, border: DrawStyle = new Color(255, 255, 255, 128)) extends LayerRenderer {

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
       }).flatten.map( _ * Canvas.scale ).toArray )
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
      }).flatten.map( _ * Canvas.scale ).toArray )
    poly.setClosed(false)
    poly
  }

  override def prepareRender(subWorld: SubWorld, layer: Int) {}

  override def render(g: Graphics) {
    border.applyOnto(g)

    horizontal.foreach( g.draw )
    vertical.foreach( g.draw )
  }

  override def toString: String = "Map grid"

}
