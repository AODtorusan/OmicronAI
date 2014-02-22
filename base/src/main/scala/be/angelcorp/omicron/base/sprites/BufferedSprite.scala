package be.angelcorp.omicron.base.sprites

import java.awt.image.BufferedImage
import org.newdawn.slick.Image

class BufferedSprite( val key: String, val bufferedSprite: BufferedImage ) extends Sprite {

  override def dependencies: Iterable[BufferedSprite] = Nil
  override def image: Image = SpriteUtils.toImage( bufferedSprite )

}

object BufferedSprite {

  def apply( sprite: Sprite ): BufferedSprite = sprite match {
    case bs: BufferedSprite => bs
    case _ => new BufferedSprite(sprite.key, SpriteUtils.toBufferedImage(sprite.image))
  }

}
