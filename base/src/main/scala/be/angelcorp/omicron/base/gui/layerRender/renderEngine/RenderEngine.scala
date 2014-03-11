package be.angelcorp.omicron.base.gui.layerRender.renderEngine

import scala.collection.mutable
import scala.collection.immutable
import org.newdawn.slick.Graphics
import be.angelcorp.omicron.base.gui.layerRender.LayerRenderer
import be.angelcorp.omicron.base.gui.slick.SpriteBatch
import be.angelcorp.omicron.base.world._
import be.angelcorp.omicron.base.sprites.Sprite
import be.angelcorp.omicron.base.gui.ViewPort

class RenderEngine extends LayerRenderer {
  type SpriteLayer = Int

  val openglRenderer = new SpriteBatch()

  /** List( List( (sprite, x, y, rotation) ) ) */
  var batches : Map[SpriteLayer, List[ Iterable[(Sprite, Float, Float, Float)]] ] = Map.empty

  /** Engine sprite providers */
  val spriteProvider = mutable.ListBuffer[SpriteProvider]()

  // LayerRenderer's that are rendered after  the sprite layers
  val overlays = mutable.Map[SpriteLayer, mutable.ListBuffer[LayerRenderer]]()

  override def viewChanged(view: ViewPort) = {
    overlays.values.flatten.par.foreach( _.viewChanged(view) )
  }

  override def prepareRender(subWorld: SubWorld, layer: Int) = {
    overlays.values.flatten.par.foreach( _.prepareRender(subWorld, layer) )

    val states  = subWorld.states.flatten.toList
    val sprites = mutable.ListBuffer[(SpriteLayer, (Sprite, Float, Float, Float))]()
    spriteProvider.foreach( provider =>
      sprites ++= provider.sprites( states, layer )
    )
    batches = toBatch(sprites).filter( _._1 < RenderEngine.unit2spriteLayer(layer + 1) )
  }

  def toBatch( sprites: Iterable[(SpriteLayer, (Sprite, Float, Float, Float))] ) = {
    val groupedByLayer = immutable.TreeMap( sprites.groupBy( _._1 ).toSeq: _* )
    val groupedByTexture = groupedByLayer.mapValues( _.groupBy( _._2._1.image.getTexture ) )
    groupedByTexture.mapValues( _.values.toList.map( _.map( _._2 ) ) )
  }

  override def render(g: Graphics) {
    val layers = (batches.keys ++ overlays.keys).toList.distinct.sorted
    for (layer <- layers) {
      val layerBatches = batches.getOrElse(layer, Nil)
      for (list <- layerBatches; (sprite, x, y, theta) <- list) {
        val img = sprite.image
        openglRenderer.drawImage( img, x - img.getWidth/2, y - img.getHeight/2, theta)
      }
      openglRenderer.flush()
      overlays.getOrElse(layer, Nil).foreach( _.render(g) )
    }
  }

}

object RenderEngine {

  def unit2spriteLayer(layer: Int) = layer match {
    case 0 => Ground
    case 1 => Air
    case 2 => Space
    case _ => 100
  }
  def unitShadow2spriteLayer(layer: Int) = layer match {
    case 0 => GroundShadow
    case 1 => AirShadow
    case 2 => SpaceShadow
    case _ => 100
  }

  val SubTerrain    = 10
  val Terrain       = 20
  val GroundShadow  = 30
  val Ground        = 40
  val AirShadow     = 50
  val Air           = 60
  val SpaceShadow   = 70
  val Space         = 80
  val AboveSpace    = 90
}