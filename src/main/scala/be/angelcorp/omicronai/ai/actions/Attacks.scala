package be.angelcorp.omicronai.ai.actions

import org.newdawn.slick.{Color, Graphics}
import com.lyndir.omicron.api.model.WeaponModule
import be.angelcorp.omicronai.{HexTile, Location}
import be.angelcorp.omicronai.ai.{ActionExecutionException, ActionExecutor}
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.gui.layerRender.LayerRenderer
import be.angelcorp.omicronai.gui.{Canvas, ViewPort}
import scala.concurrent.ExecutionContext

case class AttackAction( asset: Asset, module: WeaponModule, destination: Location ) extends Action {

  val preview = new LayerRenderer {
    val (toX, toY) = (destination: HexTile).centerXY
    def render(g: Graphics, view: ViewPort) {
      val (fromX, fromY) = (asset.location: HexTile).centerXY
      g.setColor( Color.red )
      g.drawLine(fromX * Canvas.scale, fromY * Canvas.scale, toX * Canvas.scale, toY * Canvas.scale)
    }
  }

  override def execute(ai: ActionExecutor)(implicit context: ExecutionContext) =
    wasSuccess( ai.attack(asset, module, destination) )(ai.executionContext)

  override def recover(failure: ActionExecutionException) = Some(this)

}
