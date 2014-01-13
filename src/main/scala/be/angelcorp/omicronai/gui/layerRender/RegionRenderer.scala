package be.angelcorp.omicronai.gui.layerRender

import math._
import collection.mutable
import org.newdawn.slick.{Color, Graphics}
import be.angelcorp.omicronai.gui.{DrawStyle, GuiTile, ViewPort}
import scala.collection.mutable.ListBuffer
import org.newdawn.slick.geom.Polygon
import be.angelcorp.omicronai.{HexTileEdge, HexTile, RegionOfInterest}
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

class RegionRenderer(val roi:    RegionOfInterest,
                     val border: DrawStyle = new DrawStyle(Color.red, 3.0f),
                     val fill:   Color     = new Color(255,0,0,128)) extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  /** Line segment between two data points  */
  class Segment(val x0: Float, val y0: Float, val x1: Float, val y1: Float) {
    override def equals(obj: Any) = obj match {
      case other: Segment => (x0 == other.x0 && y0 == other.y0 && x1 == other.x1 && y1 == other.y1) ||
                             (x1 == other.x0 && y1 == other.y0 && x0 == other.x1 && y0 == other.y1)
      case _ => false
    }
    override def hashCode() = (x0, y0).hashCode ^ (x1, y1).hashCode
    override def toString = s"($x0, $y0) to ($x1, $y1)"
  }

  val shapes = {
    // Find all the tile segments defining the contour
    // For each tile we add all the required contour segments to a list
    // If the list already contains that segment, it is removed
    // This results in a collection of segments that define the outer contour(s) of the ROI
    val segments = mutable.Set[HexTileEdge]()
    roi.tiles.foreach( location => {
      val tile = location: HexTile
      tile.edgesXY.foreach( edge => if (!segments.add(edge)) segments.remove(edge) )
    } )

    // Join the spectate segments into polygons for improved render speed and fill capabilities
    // This simply walks along all the segments until a closed contour is found
    val eps = 1E-4f
    val shapes = ListBuffer[Polygon]()
    while (segments.nonEmpty) {
      val start = segments.head
      segments.remove(start)

      val endPoint = start.startXY
      var nowPoint = start.endXY

      val points = ListBuffer[(Float, Float)]( endPoint )
      while ( nowPoint != endPoint && segments.nonEmpty ) {
        points.append(nowPoint)
        // Find the next segment, touching the current one
        segments.find( s => {
          val (x0, y0) = s.startXY
          val (x1, y1) = s.endXY
          abs(x0-nowPoint._1) < eps && abs(y0-nowPoint._2) < eps || abs(x1-nowPoint._1) < eps && abs(y1-nowPoint._2) < eps
        } ) match {
          case Some(p) =>
            val (x0, y0) = p.startXY
            val (x1, y1) = p.endXY
            nowPoint = if (abs(x0-nowPoint._1) < eps) (x1, y1) else (x0, y0)
            segments.remove(p)
          case _ =>
            logger.warn("Gui, region contour not closed!", new Exception("Cannot find closed contour of region shape!"))
            segments.clear()
        }
      }

      shapes.append( new Polygon( points.map( v => Seq( v._1 * GuiTile.scale, v._2 * GuiTile.scale ) ).flatten.toArray ) )
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
