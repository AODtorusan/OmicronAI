package be.angelcorp.omicronai.gui.layerRender

import math._
import collection.mutable
import org.newdawn.slick.{Color, Graphics}
import be.angelcorp.omicronai.gui.{DrawStyle, GuiTile, ViewPort}
import be.angelcorp.omicronai.goals.RegionOfInterest
import scala.collection.mutable.ListBuffer
import org.newdawn.slick.geom.Polygon

class RegionRenderer(val roi:    RegionOfInterest,
                     val border: DrawStyle = new DrawStyle(Color.red, 3.0f),
                     val fill:   Color     = new Color(255,0,0,128)) extends LayerRenderer {

  /** Line segment between two data points  */
  class Segment(val x0: Float, val y0: Float, val x1: Float, val y1: Float) {
    override def equals(obj: Any) = obj match {
      case other: Segment => (x0 == other.x0 && y0 == other.y0 && x1 == other.x1 && y1 == other.y1) ||
                             (x1 == other.x0 && y1 == other.y0 && x0 == other.x1 && y0 == other.y1)
      case _ => false
    }
    override def hashCode() = (x0, y0).hashCode ^ (x1, y1).hashCode
  }

  val shapes = {
    // Find all the tile segments defining the contour
    // For each tile we add all the required contour segments to a list
    // If the list already contains that segment, it is removed
    // This results in a collection of segments that define the outer contour(s) of the ROI
    val segments = mutable.Set[Segment]()
    roi.tiles.foreach( tile => {
      val vertices = GuiTile.vertices( tile ).toBuffer
      vertices.append( vertices.head )
      val tileSegments = vertices.sliding(2).map( elem => {
        val p0 = elem(0)
        val p1 = elem(1)
        new Segment( round(p0._1), round(p0._2), round(p1._1), round(p1._2) ) // <= rounding required so that == works
      } )
      tileSegments.foreach( s => if (!segments.add(s)) segments.remove(s) )
    } )

    // Join the spectate segments into polygons for improved render speed and fill capabilities
    // This simply walks along all the segments until a closed contour is found
    val shapes = ListBuffer[Polygon]()
    while (segments.nonEmpty) {
      val start = segments.head
      segments.remove(start)

      val endPoint = (start.x0, start.y0)
      var nowPoint = (start.x1, start.y1)

      val points = ListBuffer[(Float, Float)]( endPoint )
      while ( nowPoint != endPoint && segments.nonEmpty ) {
        points.append(nowPoint)
        // Find the next segment, touching the current one
        segments.find( s => s.x0 == nowPoint._1 && s.y0 == nowPoint._2 || s.x1 == nowPoint._1 && s.y1 == nowPoint._2 ) match {
          case Some(p) =>
            nowPoint = if (p.x0 == nowPoint._1) (p.x1, p.y1) else (p.x0, p.y0)
            segments.remove(p)
          case _ => throw new Exception("Contour not closed")
        }
      }

      shapes.append( new Polygon( points.map( v => Seq( v._1, v._2 ) ).flatten.toArray ) )
    }
    shapes.toList
  }

  def render(g: Graphics, view: ViewPort) {
    // Draw the border if required
    if (border.color != Color.transparent) {
      border.applyOnto(g)
      shapes.foreach( g.draw )
    }

    // Draw the fill if required
    if (fill != Color.transparent) {
      g.setColor( fill )
      shapes.foreach( g.fill )
    }
  }

  override val toString = roi.toString
}
