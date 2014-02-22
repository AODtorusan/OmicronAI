package be.angelcorp.omicron.base.sprites

import java.util.regex.Pattern
import scala.collection.mutable
import scala.collection.JavaConverters._
import org.newdawn.slick.util.ResourceLoader
import org.reflections.Reflections
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder}
import org.reflections.scanners.ResourcesScanner
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import com.typesafe.config.{ConfigFactory, Config}
import java.net.URL
import be.angelcorp.omicron.base.configuration.Configuration.config
import java.nio.file._
import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes

object Sprites  extends SpriteContext {
  protected val logger = Logger( LoggerFactory.getLogger( getClass ) )

  private val texCfgCache = mutable.Map[String, Config]()
  private val texPattern  = Pattern.compile(".*\\.tex")

  def load( urls: java.util.Collection[URL] ) {
    val texConfigPaths = new Reflections(new ConfigurationBuilder()
      .addUrls( urls )
      .setScanners(new ResourcesScanner())
    ).getResources(texPattern).asScala
    for (path <- texConfigPaths) {
      val config = ConfigFactory.parseURL( ResourceLoader.getResource(path) )
      load( config )
    }
  }
  def load( path: Path ) {
    Files.walkFileTree(path, new FileVisitor[Path] {
      override def postVisitDirectory(dir: Path, exc: IOException) = FileVisitResult.CONTINUE
      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = FileVisitResult.CONTINUE
      override def visitFile(file: Path, attrs: BasicFileAttributes) = {
        if (texPattern.matcher( file.getFileName.toString ).matches()) {
          val config = ConfigFactory.parseFile( file.toFile )
          load( config )
        }
        FileVisitResult.CONTINUE
      }
      override def visitFileFailed(file: Path, exc: IOException) = FileVisitResult.CONTINUE
    })
  }

  def load( urls: Iterable[URL] ): Unit =
    load( urls.asJavaCollection )

  def load( urls: URL* ): Unit =
    load( urls.toList.asJavaCollection )

  def load(): Unit = {
    load( ClasspathHelper.forClassLoader() )
    load( config.cachePath )
  }

  override def findSpriteConfig(key: String): Option[Config] = texCfgCache.get(key)

  def load( config: Config ) {
    if (config.hasPath("textures"))
      config.getConfigList("textures").asScala.foreach( c =>
        texCfgCache += c.getString("key") -> c
      )
    else
      texCfgCache += config.getString("key") -> config
  }

}

