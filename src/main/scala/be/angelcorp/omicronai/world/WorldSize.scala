package be.angelcorp.omicronai.world

import be.angelcorp.omicronai.Location
import com.lyndir.omicron.api.model.Size

case class WorldSize(uSize: Int, vSize: Int, hSize: Int = 3) {

  def locations =
    for (u <- 0 until uSize;
         v <- 0 until vSize;
         h <- 0 until hSize ) yield new Location(u, v, h, this)

  def inBounds(l: Location): Boolean =
    l.u >= 0 && l.v >= 0 && l.h >= 0 && l.u < uSize && l.v < vSize && l.h < hSize

}

object WorldSize {

  implicit def worldsize2size( s: WorldSize ): Size = new Size(s.uSize, s.vSize)
  implicit def size2worldsize( s: Size ): WorldSize = new WorldSize(s.getWidth, s.getHeight)


}