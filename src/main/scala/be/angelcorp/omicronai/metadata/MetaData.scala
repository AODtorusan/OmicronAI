package be.angelcorp.omicronai.metadata

import be.angelcorp.omicronai.gui.layerRender.{PolyLineRenderer, HeatMapRenderer, GridRenderer, LayerRenderer}
import be.angelcorp.omicronai.algorithms.AStarSolution
import org.newdawn.slick.Color
import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.gui.slick.DrawStyle

trait MetaData {

  def title: String
  def description: String = title

  def layers: Map[String, LayerRenderer]

}

class PathMetadata(path: Seq[Location], val title: String) extends MetaData {
  def layers: Map[String, LayerRenderer] = Map[String, LayerRenderer](
    "Path" -> new PolyLineRenderer(path, new DrawStyle(Color.yellow, 3.0f))
  )
}

class PathfinderMetadata(solution: AStarSolution, others: Seq[AStarSolution]) extends MetaData {

  val title = "Movement pathfinder data"

  def layers: Map[String, LayerRenderer] = {
    val layers = Map.newBuilder[String, LayerRenderer]

    layers += (("F value", new HeatMapRenderer( others.map( s => (s.tile, s.f) ).toMap )))
    layers += (("G value", new HeatMapRenderer( others.map( s => (s.tile, s.g) ).toMap )))
    layers += (("H value", new HeatMapRenderer( others.map( s => (s.tile, s.h) ).toMap )))

    layers += (("Solution path", new PolyLineRenderer(solution.path, new DrawStyle(Color.yellow, 3.0f) ) ))

    layers.result()
  }

}