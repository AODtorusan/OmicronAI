package be.angelcorp.omicron.base.gui.textures

import java.net.URL
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import javax.imageio.ImageIO
import com.typesafe.config.{ConfigFactory, Config}
import org.newdawn.slick.Image
import org.newdawn.slick.util.{BufferedImageUtil, ResourceLoader}
import be.angelcorp.omicron.base.configuration.ConfigHelpers._
import be.angelcorp.omicron.base.gui.Canvas

trait TextureConfig {

  def key: String

  def image: Image

  def config: Config

  def mkConfig[K <: AnyRef](p: Iterable[(String, K)]): Config = {
    val properties = new java.util.HashMap[String, K]()
    p.foreach( e => properties.put(e._1, e._2) )
    properties.put("type", getClass.getSimpleName.asInstanceOf[K])
    ConfigFactory.parseMap( properties )
  }

  def mkConfig[K <: AnyRef](param: (String, K)*): Config = mkConfig(param)

}

object TextureConfig {

  def parse(cfg: Config): Try[Iterable[TextureConfig]] = {
    cfg.getOptionalConfigList( "textures" ) match {
      case Some( textureConfigs ) =>
        Success(
          (for (texConf <- textureConfigs.asScala ) yield parse(texConf).getOrElse(Nil)).flatten
        )
      case _ =>
        if (cfg.hasPath("type"))
          cfg.getString("type") match {
            case s if s == classOf[BlendedTexture].getSimpleName  => Success(Seq( new BlendedTexture(cfg)   ))
            case s if s == classOf[CutTexture].getSimpleName      => Success(Seq( new CutTexture(cfg)       ))
            case s if s == classOf[EmbeddedTexture].getSimpleName => Success(Seq( new EmbeddedTexture(cfg)  ))
            case s if s == classOf[SimpleTexture].getSimpleName   => Success(Seq( new SimpleTexture(cfg)    ))
            case _ => Failure( new Exception(s"Could not find a valid 'type' in $cfg") )
          }
        else Failure( new Exception(s"Failed to find valid 'type' field in config $cfg") )
    }
  }

  def parse(path: URL): Try[Iterable[TextureConfig]] = {
    val cfg = ConfigFactory.parseURL( path )
    parse(cfg)
  }

}

case class SimpleTexture( key: String, source: String, tileSize: Option[Int] ) extends TextureConfig {

  def this(config: Config) = this(
    config.getString("key"),
    config.getString("source"),
    config.getOptionalInt("tileSize")
  )

  def image = {
    val img = new Image( source )
    tileSize match {
      case Some(sz) =>
        val xSize = img.getWidth  * Canvas.scale / sz
        val ySize = img.getHeight * Canvas.scale / sz
        img.getScaledCopy( xSize.toInt, ySize.toInt)
      case None => img
    }
  }

  def config = mkConfig(
    "key"    -> key     ::
    "source" -> source  ::
    tileSize.map( x => "tileSize" -> Int.box(x) ).toList
  )

}

case class EmbeddedTexture( key: String, sourceKey: String, x: Int, y: Int, width: Int, height: Int ) extends TextureConfig {

  def this(config: Config) = this(
    config.getString("key"),
    config.getString("sourceKey"),
    config.getInt("x"),
    config.getInt("y"),
    config.getInt("width"),
    config.getInt("height")
  )

  def image = Textures.get(key) match {
    case Some(img) => img.getSubImage(x, y, width, height)
    case None => ???
  }

  def config = mkConfig(
    "key"       -> key,
    "sourceKey" -> sourceKey,
    "x"         -> Int.box(x),
    "y"         -> Int.box(y),
    "width"     -> Int.box(width),
    "height"    -> Int.box(height)
  )

}

case class CutTexture( key: String, source: String, centerX: Int, centerY: Int, size: Int ) extends TextureConfig{

  def this(config: Config) = this(
    config.getString("key"),
    config.getString("source"),
    config.getInt("centerX"),
    config.getInt("centerY"),
    config.getInt("size")
  )

  def config = mkConfig(
    "key"         -> key,
    "source"      -> source,
    "centerX"     -> Int.box(centerX),
    "centerY"     -> Int.box(centerY),
    "size"        -> Int.box(size)
  )

  def image = {
    val url   = ResourceLoader.getResource( source )
    val sourceImg = ImageIO.read( url )
    val tile  = TextureUtils.cutTile(sourceImg, centerX, centerY, size)
    val tex   = TextureUtils.toTexture(tile)
    val sz    = (size * (Canvas.scale / size)).toInt
    tex.getScaledCopy(sz, sz)
  }

}

case class BlendedTexture( key: String, source: String, centerX: Int, centerY: Int, size: Int, blendradius: Int ) extends TextureConfig{

  def this(config: Config) = this(
    config.getString("key"),
    config.getString("source"),
    config.getInt("centerX"),
    config.getInt("centerY"),
    config.getInt("size"),
    config.getInt("blendradius")
  )

  def config = mkConfig(
    "key"         -> key,
    "source"      -> source,
    "centerX"     -> Int.box(centerX),
    "centerY"     -> Int.box(centerY),
    "size"        -> Int.box(size),
    "blendradius" -> Int.box(blendradius)
  )

  def image = {
    val url   = ResourceLoader.getResource( source )
    val sourceImg = ImageIO.read( url )
    val tile  = TextureUtils.cutTile(sourceImg, centerX, centerY, size, blendradius)
    val tex   = BufferedImageUtil.getTexture(source, tile)
    val sz    = ((size + blendradius) * (Canvas.scale / size)).toInt
    new Image(tex).getScaledCopy(sz, sz)
  }

}
