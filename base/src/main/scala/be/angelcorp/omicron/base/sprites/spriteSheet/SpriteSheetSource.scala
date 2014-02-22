package be.angelcorp.omicron.base.sprites.spriteSheet

import java.awt.image.BufferedImage
import java.awt.{Color, Graphics2D, Rectangle}
import java.awt.geom.AffineTransform
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import be.angelcorp.omicron.base.sprites.{BufferedSprite, SpriteUtils, Sprite}
import be.angelcorp.omicron.base.configuration.ConfigHelpers._
import scala.collection.JavaConverters._
import com.typesafe.config.ConfigFactory

class SpriteSheetSource( val width: Int, val height: Int, val mapping: Map[BufferedSprite, Rectangle] ) {
  private val logger = Logger( LoggerFactory.getLogger( getClass ) )

  lazy val image = {
    val result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = result.getGraphics.asInstanceOf[Graphics2D]
    for( (sprite, place) <- mapping ) {
      val img = sprite.bufferedSprite
      if (isRotated( sprite, place )) {
        logger.info("Found rotated image ...")
        val transform = new AffineTransform()
        transform.rotate(math.Pi / 2)
        transform.translate(place.x, place.y)
        g.drawImage(img, transform, null)
        g.setColor( new Color(0,0,255) )
        g.drawRect(place.x, place.y, place.width, place.height)
      } else {
        g.drawImage(img, place.x, place.y, null)
        g.setColor( new Color(255,0,0) )
        g.drawRect(place.x, place.y, place.width, place.height)
      }
    }
    result
  }

  private def isRotated( sprite: BufferedSprite, place: Rectangle ) =
    sprite.bufferedSprite.getWidth != place.width

  def metadata(path: String, key: String = "spriteSheet", additionalOptions: Sprite => Map[String, Any] = Map.empty) = {
    val sprites = mapping.map { case (sprite, location) =>
      Map(
        "key" -> s"$key.${sprite.key}",
        "source" -> s"sprite://$key",
        "x" -> location.x,
        "y" -> location.y,
        "width" -> location.width,
        "height" -> location.height,
        "rotation" -> (if (isRotated(sprite, location)) 90 else 0)
      ).++(additionalOptions(sprite)).asJava
    }.toList

    val allSprites = (Map(
      "key" -> key,
      "source" -> path
    ).asJava :: sprites).asJava

    ConfigFactory.parseMap( Map("textures" -> allSprites).asJava )
  }

}
