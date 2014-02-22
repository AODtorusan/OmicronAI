package be.angelcorp.omicron.base.sprites

import org.lwjgl.util.Timer

case class AnimatedSprite(key: String, frames: IndexedSeq[Sprite], fps: Float = 24) extends Sprite {

  private lazy val timer = new Timer()
  private var i = 0

  override def dependencies = frames

  override def image = {
    if (timer.getTime > 1 / fps) {
      i = (i + 1) % frames.size
      timer.reset()
    }
    frames(i).image
  }

}
