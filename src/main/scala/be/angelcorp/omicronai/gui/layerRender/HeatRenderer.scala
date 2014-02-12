package be.angelcorp.omicronai.gui.layerRender

import be.angelcorp.omicronai.{TileCollection, Location}
import org.newdawn.slick.{Graphics, Color}
import be.angelcorp.omicronai.gui.ViewPort
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.world.SubWorld

trait HeatRenderer extends LayerRenderer {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  var lastScale  =  Float.NaN
  var lastOffset = (Float.NaN, Float.NaN)

  /** Number of quantization levels (bins) to use for the data */
  def quantisation = 4
  /** Get the numerical value for a specific tile, NaN = no data = bin -1 */
  def valueFor( location: Location ): Double

  def value2color(value: Double, bin: Int, minValue: Double, maxValue: Double): Color = {
    if (value.isNaN)
      Color.transparent
    else {
    val minColor = Color.white
    val maxColor = Color.blue
    val z = bin / (quantisation - 1.0f)
    new Color( minColor.r + (maxColor.r - minColor.r) * z,
               minColor.g + (maxColor.g - minColor.g) * z,
               minColor.b + (maxColor.b - minColor.b) * z,
               minColor.a + (maxColor.a - minColor.a) * z)
  } }

  var regions: Iterable[RegionRenderer] = Nil

  override def viewChanged(view: ViewPort) {
    logger.info("Updating heatmap")
    val values = view.tilesInView.map( l => (l, valueFor(l)) )
    val valuesNonNan = values.filterNot( _._2.isNaN )
    if (valuesNonNan.nonEmpty) {
      val min = valuesNonNan.minBy( _._2 )._2
      val max = valuesNonNan.maxBy( _._2 )._2
      val binned = values.groupBy( entry => {
        val d = entry._2
        if ( d.isNaN ) -1 else ((d - min) / (max - min) * quantisation).toInt
      } )
      regions = binned.map( entry => {
        val bin = entry._1
        val tiles = entry._2.map(_._1)
        val centerValue = if (bin == -1) Double.NaN else min + ((max - min) / quantisation) * (bin + 0.5)
        val color = value2color(centerValue, bin, min, max)

        val roi = new TileCollection( tiles.toSet )

        new RegionRenderer(roi, Color.yellow, color)
      } )
    } else regions = Nil
    lastScale  = view.scale
    lastOffset = view.offset
  }

  override def prepareRender(subWorld: SubWorld, layer: Int) {}

  def render(g: Graphics) {
    regions.foreach( _.render(g) )
  }

}

class HeatMapRenderer( val locations: Map[Location, Double], override val quantisation: Int = 4 ) extends HeatRenderer {

  def valueFor(location: Location) = locations.getOrElse(location, Double.NaN)

}
