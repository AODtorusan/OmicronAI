package be.angelcorp.omicron.base.gui.layerRender

import scala.collection.mutable.ListBuffer
import org.newdawn.slick.Graphics
import org.newdawn.slick.geom.Polygon
import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.base.gui.Canvas
import be.angelcorp.omicron.base.gui.slick.DrawStyle
import be.angelcorp.omicron.base.world.SubWorld

class PolyLineRenderer(path: Seq[Location], style: DrawStyle, description: String = "PolyLine") extends LayerRenderer {

  lazy val polyLines = {
    val polyLines = ListBuffer[Polygon]()

    var poly = new Polygon()
    poly.setClosed(false)
    polyLines.append(poly)

    var last: Option[Location] = None
    for ( loc <- path ) {
      // Avoid a line crossing the map when wrapping
      if (last.isDefined && (loc.bounds.getWidth / 2 < math.abs(last.get.δuUnwrap(loc)) || loc.bounds.getWidth / 2 < math.abs(last.get.δvUnwrap(loc)) ) ) {
        // Add the next line segment (from onto the map to out-of-map)
        val virtualNext = loc.mirrors.minBy( mirror => mirror δunwrap last.get )
        val centerNext  = Canvas.center(virtualNext)
        poly.addPoint( centerNext._1, centerNext._2 )

        // Start a new line
        poly = new Polygon()
        poly.setClosed(false)
        polyLines.append(poly)

        // Add the out-of-map equivalent to location to the last line segment (from out-of-map onto the map)
        val trueLast = last.get.mirrors.minBy( mirror => mirror δunwrap loc )
        val center = Canvas.center(trueLast)
        poly.addPoint( center._1, center._2 )
      }

      // Add the next vertex to the polyline
      val center = Canvas.center(loc)
      poly.addPoint( center._1, center._2 )

      last = Some(loc)
    }
    polyLines
  }

  override def prepareRender(subWorld: SubWorld, layer: Int) {}

  override def render(g: Graphics) {
    style.applyOnto(g)
    polyLines.foreach( g.draw )
  }

  override def toString = description

}
