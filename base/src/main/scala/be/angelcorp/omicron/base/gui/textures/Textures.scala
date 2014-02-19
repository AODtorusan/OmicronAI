package be.angelcorp.omicron.base.gui.textures

import java.util.regex.Pattern
import scala.util.{Failure, Success}
import scala.collection.mutable
import scala.collection.JavaConverters._
import org.newdawn.slick.Image
import org.reflections.Reflections
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder}
import org.reflections.scanners.ResourcesScanner
import org.slf4j.LoggerFactory
import org.newdawn.slick.util.ResourceLoader
import com.typesafe.scalalogging.slf4j.Logger


object Textures {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  private val imageCache  = mutable.Map[String, Image]()
  private val texCfgCache = mutable.Map[String, TextureConfig]()

  def load() {
    val texConfigPaths = new Reflections(new ConfigurationBuilder()
      .setUrls( ClasspathHelper.forClassLoader() )
      .setScanners(new ResourcesScanner())
    ).getResources(Pattern.compile(".*\\.tex")).asScala
    for (path <- texConfigPaths) {
      TextureConfig.parse( ResourceLoader.getResource(path) ) match {
        case Success(cfgs) =>
          logger.debug(s"Loaded texture configuration from $path")
          for (cfg <- cfgs)
            texCfgCache += cfg.key -> cfg
        case Failure(e) =>
          logger.info(s"Failed to load texture configuration from path $path", e)
      }
    }
  }

  def get(key: String): Option[Image] = imageCache.get(key) orElse texCfgCache.get(key).map(cfg => get(cfg))

  def get(tex: TextureConfig): Image = imageCache.getOrElseUpdate(tex.key, tex.image)

}
