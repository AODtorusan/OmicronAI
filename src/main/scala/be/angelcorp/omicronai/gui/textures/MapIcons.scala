package be.angelcorp.omicronai.gui.textures

import scala.collection.mutable
import be.angelcorp.omicronai.assets.Asset
import com.lyndir.omicron.api.model.{IUnitType, IGameObject}
import org.newdawn.slick.Image

object MapIcons {

  private val cache = mutable.Map[IUnitType, Image]()

  def getIcon( asset: Asset ): Image = getIcon( asset.gameObject.getType )

  def getIcon( obj: IGameObject ): Image = getIcon( obj.getType )

  def getIcon( typ: IUnitType ): Image = cache.getOrElseUpdate( typ, {
    typ.getTypeName match {
      case "Airship"            => Textures.get("vehicles.airship").get
      case "Engineer"           => Textures.get("vehicles.engineer").get
      case "Quarry"             => Textures.get("buildings.quarry").get
      case "Scout"              => Textures.get("vehicles.scout").get

      case "Construction Site"  => Textures.get("buildings.construction_site").get
      case "Container"          => Textures.get("buildings.container").get
      case "Drill Site"         => Textures.get("buildings.drill_site").get

      case _ => ???
    }
  } )

}
