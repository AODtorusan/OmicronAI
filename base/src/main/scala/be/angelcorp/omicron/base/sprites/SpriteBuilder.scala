package be.angelcorp.omicron.base.sprites

import java.net.URI
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Success, Failure, Try}
import org.slf4j.LoggerFactory
import org.newdawn.slick.Image
import org.newdawn.slick.opengl.InternalTextureLoader
import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicron.base.configuration.ConfigHelpers._
import be.angelcorp.omicron.base.gui.Canvas

class SpriteLoader {
  private val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def loadStaticSprite( context: SpriteContext, key: String, source: URI, c: Config ): Sprite = {
    val image = context.findImage(source).getOrElse( throw new MissingSpriteException(source) )
    val processors = mutable.ListBuffer[Image => Image]()

    if (c.hasPath("x") || c.hasPath("y") || c.hasPath("width") || c.hasPath("height")) {
      val x      = c.getOptionalInt("x") getOrElse 0
      val y      = c.getOptionalInt("y") getOrElse 0
      val width  = c.getOptionalInt("width")
      val height = c.getOptionalInt("height")
      processors += (i => {
        val w = width.getOrElse(i.getWidth - x)
        val h = height.getOrElse(i.getHeight - y)
        logger.debug(s"Taking sub-image for texture $key: {x=$x, y=$y, width=$w, height=$h}")
        i.getSubImage(x, y, w, h)
      } )
    }
    c.getOptionalInt("tileHeight").foreach( tileHeight => {
      logger.debug(s"Resizing texture $key with from a tile height of $tileHeight to ${Canvas.scale} (a factor ${Canvas.scale / tileHeight})")
      processors += (_.getScaledCopy( Canvas.scale / tileHeight ))
    } )
    c.getOptionalDouble("scale").foreach( scale => {
      logger.debug(s"Resizing texture $key by $scale")
      processors += (_.getScaledCopy( scale.toFloat ))
    } )
    new StaticSprite( key, processors.foldLeft( image )((image, processor) => processor(image)) )
  }

  def loadAnimatedSprite( context: SpriteContext, key: String, frameSources: Seq[URI], c: Config ): Sprite = {
    val frames = frameSources.map( source => context.findSprite(source).getOrElse( ??? ) )
    val fps = c.getOptionalInt("fps").getOrElse(24)
    new AnimatedSprite(key, frames.toIndexedSeq, fps)
  }

  def loadSprite( context: SpriteContext, c: Config ): Try[Sprite] = {
    try {
      val key = c.getString("key")
      c.getOptionalString("source") match {
        case Some(source) =>
          Success( loadStaticSprite(context, key, URI.create(source), c) )
        case None =>
          c.getOptionalStringList( "source" ) match {
            case Some( frames ) =>
              Success( loadAnimatedSprite(context, key, frames.asScala.map( URI.create ), c) )
            case None =>
              Failure( new Exception(s"Failed to find/process 'source' field in sprite configuration: $c") )
          }
      }
    } catch {
      case e: Throwable => Failure(e)
    }
  }

}

abstract class SpriteContext {
  protected def logger: Logger
  import SpriteContext._

  private val cache   = mutable.Map[String, Sprite]()

  val loader = new SpriteLoader

  def addSprite( sprite: Sprite ): Unit =
    addSprite(sprite.key, sprite)

  def addSprite( key: String, sprite: Sprite ): Unit =
    cache += key -> sprite

  def loadedSprites = cache.values

  def findImage(uri: URI): Try[Image] = {
    val key = uri2key(uri)
    uri.getScheme match {
      case "sprite" => findSprite( key ).map( _.image )
      case null     =>
        Success( new Image( key ) )
      case scheme   =>
        val stream  = uri.toURL.openStream()
        val texture = InternalTextureLoader.get().getTexture( stream, "png", false, Image.FILTER_LINEAR )
        Success( new Image( texture ) )
    }
  }

  def findSprite(uri: URI): Try[Sprite] = {
    val key = uri2key(uri)
    uri.getScheme match {
      case "sprite" => findSprite( key )
      case _ => findImage(uri).map( img => new StaticSprite( key, img ) )
    }
  }

  def findSpriteConfig(key: String): Option[Config]

  def findSprite(key: String): Try[Sprite] = {
    cache.get(key) match {
      case Some(sprite) => Success(sprite)
      case None =>
        val sprite = findSpriteConfig(key).map( config => loader.loadSprite(this, config)).getOrElse(
          Failure(new Exception(s"No configuration found for texture key $key"))
        )
        sprite.foreach( sprite => cache += key -> sprite )
        sprite
    }
  }

}

object SpriteContext {
  def uri2key( uri: URI ): String = if (uri.getScheme != null) uri.getAuthority else uri.getRawPath
  def key2sprite( key: String ): URI = URI.create("sprite://" + key)
}

class MissingSpriteException( val uri: URI, cause: Throwable = null )
  extends RuntimeException(s"Failed to load sprite from $uri", cause)

trait SpriteProcessor {

  def process( sprite: Image ): Image

}
