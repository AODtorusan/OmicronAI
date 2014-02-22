package be.angelcorp.omicron.base.sprites

import org.newdawn.slick.Image
import java.awt.image.BufferedImage

case class StaticSprite( key: String, image: Image ) extends Sprite {

  override def dependencies: Iterable[Sprite] = Nil

}

object StaticSprite {

  def apply( key: String, bufferedImage: BufferedImage ) =
    new StaticSprite(key, SpriteUtils.toImage( bufferedImage ))

}
