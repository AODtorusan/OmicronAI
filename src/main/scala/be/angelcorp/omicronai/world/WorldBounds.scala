package be.angelcorp.omicronai.world

import be.angelcorp.omicronai.Location
import com.lyndir.omicron.api.model.Size

case class WorldBounds(uSize: Int, vSize: Int, hSize: Int = 3, u0: Int = 0, v0: Int = 0, h0: Int = 0) {

  def locations =
    for (u <- u0 until (u0+uSize);
         v <- v0 until (v0+vSize);
         h <- h0 until (h0+hSize) ) yield new Location(u, v, h, this)

  def inBounds(l: Location): Boolean =
    l.u >= u0 && l.v >= v0 && l.h >= h0 && l.u < u0 + uSize && l.v < v0 + vSize && l.h < h0 + hSize

}

object WorldBounds {

  implicit def worldsize2size( s: WorldBounds ): Size = new Size(s.uSize, s.vSize)
  implicit def size2worldsize( s: Size ): WorldBounds = new WorldBounds(s.getWidth, s.getHeight)


}