package be.angelcorp.omicronai.gui.layerRender

import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.gui.{GuiTile, ViewPort, DrawStyle}
import org.newdawn.slick.{Color, Graphics}
import org.newdawn.slick.geom.Polygon
import scala.collection.mutable.ListBuffer

class PolyLineRenderer(path: Seq[Location], style: DrawStyle, description: String = "PolyLine") extends LayerRenderer {

  lazy val polyLines = {
    val polyLines = ListBuffer[Polygon]()

    var poly = new Polygon()
    poly.setClosed(false)
    polyLines.append(poly)

    var last: Option[Location] = None
    for ( loc <- path ) {
      // Avoid a line crossing the map when wrapping
      if (last.isDefined && (loc.size.getWidth / 2 < math.abs(last.get.δuUnwrap(loc)) || loc.size.getWidth / 2 < math.abs(last.get.δvUnwrap(loc)) ) ) {
        // Add the next line segment (from onto the map to out-of-map)
        val virtualNext = loc.mirrors.minBy( mirror => mirror δunwrap last.get )
        val centerNext = GuiTile.center(virtualNext)
        poly.addPoint( centerNext._1, centerNext._2 )

        // Start a new line
        poly = new Polygon()
        poly.setClosed(false)
        polyLines.append(poly)

        // Add the out-of-map equivalent to location to the last line segment (from out-of-map onto the map)
        val trueLast = last.get.mirrors.minBy( mirror => mirror δunwrap loc )
        val center = GuiTile.center(trueLast)
        poly.addPoint( center._1, center._2 )
      }

      // Add the next vertex to the polyline
      val center = GuiTile.center(loc)
      poly.addPoint( center._1, center._2 )

      last = Some(loc)
    }
    polyLines
  }

  def render(g: Graphics, view: ViewPort) {
    style.applyOnto(g)
    polyLines.foreach( g.draw )
  }

  override def toString = description
}
