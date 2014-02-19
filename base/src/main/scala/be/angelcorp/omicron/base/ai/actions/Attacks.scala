package be.angelcorp.omicron.base.ai.actions

import scala.concurrent.ExecutionContext
import org.newdawn.slick.{Color, Graphics}
import com.lyndir.omicron.api.model.WeaponModule
import be.angelcorp.omicron.base.{HexTile, Location}
import be.angelcorp.omicron.base.bridge.Asset
import be.angelcorp.omicron.base.gui.layerRender.LayerRenderer
import be.angelcorp.omicron.base.gui.Canvas
import be.angelcorp.omicron.base.world.SubWorld

case class AttackAction( asset: Asset, module: WeaponModule, destination: Location ) extends Action {

  val preview = new LayerRenderer {
    val (toX, toY) = (destination: HexTile).centerXY
    def render(g: Graphics) {
      val (fromX, fromY) = (asset.location: HexTile).centerXY
      g.setColor( Color.red )
      g.drawLine(fromX * Canvas.scale, fromY * Canvas.scale, toX * Canvas.scale, toY * Canvas.scale)
    }
    override def prepareRender(subWorld: SubWorld, layer: Int) {}
  }

  override def execute(ai: ActionExecutor)(implicit context: ExecutionContext) =
    wasSuccess( ai.attack(asset, module, destination) )(ai.executionContext)

  override def recover(failure: ActionExecutionException) = Some(this)

}
