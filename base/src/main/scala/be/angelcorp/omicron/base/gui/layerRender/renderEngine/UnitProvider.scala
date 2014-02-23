package be.angelcorp.omicron.base.gui.layerRender.renderEngine

import scala.Some
import com.lyndir.omicron.api.model.IGameObject
import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.base.configuration.Configuration._
import be.angelcorp.omicron.base.gui.Canvas
import be.angelcorp.omicron.base.gui.layerRender.renderEngine.RenderEngine._
import be.angelcorp.omicron.base.world.{GhostState, KnownState, WorldState}
import be.angelcorp.omicron.base.sprites.Sprite

class UnitProvider extends SpriteProvider {

  val unitSet = config.gui.unitSet

  override def sprites(states: List[(Location, WorldState)], layer: Int) = {
    val sprites = List.newBuilder[(SpriteLayer, (Sprite, Float, Float, Float))]
    states.foreach {
      case (loc, KnownState(_, Some(obj), _)) => sprites ++= generateUnitSprites(loc, obj)
      case (loc, GhostState(_, Some(obj), _)) => sprites ++= generateUnitSprites(loc, obj)
      case _ =>
    }
    sprites.result()
  }

  def generateUnitSprites(loc: Location, obj: IGameObject): Iterable[(SpriteLayer, (Sprite, Float, Float, Float))] = {
    val (x, y) = Canvas.center(loc)
    val unitGraphics = unitSet.spriteFor(obj.getType)
    unitGraphics.shadow match {
      case Some(shadow) =>
        List(
          unit2spriteLayer(loc.h)       -> (unitGraphics.unit, x, y, 0f),
          unitShadow2spriteLayer(loc.h) -> (shadow, x, y, 0f)
        )
      case _ =>
        List(unit2spriteLayer(loc.h) -> (unitGraphics.unit, x, y, 0f))
    }
  }

}
