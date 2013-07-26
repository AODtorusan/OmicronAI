package be.angelcorp.omicronai.information

import be.angelcorp.omicronai.Location
import be.angelcorp.omicronai.gui.{DrawStyle, GuiTile}
import org.newdawn.slick.Color

abstract class TileInformationLayer extends InformationLayer {

  def tiles: Seq[TileInformation]

}

class TileInformation(val location: Location, val normalizedValue: Float, val msg: String = "") {

  /** Color for normalizedValue = 1 */
  val primacyColor   = Color.yellow
  /** Color for normalizedValue = 0 */
  val secondaryColor = Color.blue

  lazy val guiTile = new GuiTile( location ) {
    override def text        = msg
    override def textColor   = Color.white
    override def fillColor   = {
      new Color(  normalizedValue * (primacyColor.r - secondaryColor.r) + secondaryColor.r,
                  normalizedValue * (primacyColor.g - secondaryColor.g) + secondaryColor.g,
                  normalizedValue * (primacyColor.b - secondaryColor.b) + secondaryColor.b,
                  normalizedValue * (primacyColor.a - secondaryColor.a) + secondaryColor.a)
    }
    override def borderStyle = Color.transparent
  }

}
