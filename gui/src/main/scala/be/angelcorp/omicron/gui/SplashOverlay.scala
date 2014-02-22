package be.angelcorp.omicron.gui

import java.io.{File, FileOutputStream}
import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.util.Random
import org.lwjgl.opengl.{Display, GL11}
import org.newdawn.slick.{Graphics, Color, GameContainer, Image}
import org.newdawn.slick.imageout.ImageIOWriter
import org.newdawn.slick.state.{StateBasedGame, BasicGameState}
import org.newdawn.slick.util.ResourceLoader
import com.typesafe.config.ConfigFactory
import be.angelcorp.omicron.base.HexTile
import be.angelcorp.omicron.base.gui.Canvas
import be.angelcorp.omicron.base.sprites.Sprites

class SplashOverlay extends BasicGameState {

  val fastSphashImage = "cache/loading.png"

  private var doUpdate         = true
  private var buffer: Image    = null
  private val tiles            = mutable.Map[HexTile, Option[Iterable[Image]]]()
  private var textures: Map[Int, mutable.Buffer[(Int, Double, Image)]] = null
  private var foundBG          = true
  private var _progress: Float = 0.1f
  private var _msg: String     = ""

  def progress(progress: Float, msg: String = "") {
    _progress = progress
    _msg      = msg
    doUpdate  = true
  }

  override def update(container: GameContainer, game: StateBasedGame, delta: Int) {
    if (doUpdate) {
      val g = buffer.getGraphics
      g.clear()
      // Draw grid
      Canvas.render(g, tiles.keys, Color.white, Color.transparent)

      // Fill in the progress tiles
      for( tile <- tiles.keys ) {
        val center = Canvas.center(tile)
        if (center._1 < buffer.getWidth * _progress || _progress >= 1f) {
          val images = tiles.get(tile).flatten match {
            case Some(imgs) => imgs
            case None       =>
              val content = randomTile(tile)
              tiles.update(tile, Some(content))
              content
          }
          for (img <- images) img.drawCentered( center._1, center._2 )
        }
      }

      val h = g.getFont.getLineHeight
      g.setColor( new Color(0, 0, 0, 128) )
      g.fillRect(0, container.getHeight - h * 2, container.getWidth, container.getHeight)
      g.setColor( Color.white )
      g.drawString(_msg, h / 2, container.getHeight - 3 * h / 2)

      doUpdate = false
    }
  }

  def randomTile(t: HexTile): Iterable[Image] = {
    for (layer <- textures.keys) yield {
      textures(layer).find( _._2 > Random.nextDouble()) match {
        case Some(entry) => entry._3
        case _ => textures(layer).head._3
      }
    }
  }

  override def render(container: GameContainer, game: StateBasedGame, g: Graphics) {
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
    buffer.draw()
  }

  override def init(container: GameContainer, game: StateBasedGame) {
    foundBG = try{
      buffer = new Image( fastSphashImage )
      buffer.draw()
      Display.swapBuffers()
      true
    } catch {
      case e: Throwable => buffer = new Image( container.getWidth, container.getHeight ); false
    }
  }

  override def leave(container: GameContainer, game: StateBasedGame) {
    buffer.destroy()
    for (texGroup <- textures; (_,_,texture) <- texGroup._2)
      texture.destroy()
  }


  override def enter(container: GameContainer, game: StateBasedGame) {
    if (buffer.isDestroyed)
      buffer = new Image( container.getWidth, container.getHeight )

    loadSimpleTextures()

    val s      = Canvas.scale
    val width  = container.getWidth
    val height = container.getHeight
    for(u <- -10 to 30; v <- -5 to 15; t = HexTile(u, v); center = Canvas.center(t)
        if -s < center._1 && center._1 < width + s && -s < center._2 && center._2 < height + s) {
      tiles += t -> None
    }

    if (!foundBG) {
      // Create the bg for the next time we start
      val g = buffer.getGraphics
      g.clear()
      Canvas.render(g, tiles.keys, Color.white, Color.transparent)
      new ImageIOWriter().saveImage( buffer, "png", new FileOutputStream( new File(fastSphashImage) ), true )
      foundBG = true
    }
  }

  def loadSimpleTextures() {
    val cfg = ConfigFactory.parseURL( ResourceLoader.getResource("splash.cfg") )

    val textureConfigs = for( entry <- cfg.getConfigList("textures").asScala ) yield
      (entry.getInt("layer"), entry.getDouble("probability"), Sprites.loader.loadSprite(Sprites, entry.getConfig("texture")).get.image)
    textures = textureConfigs.groupBy(  _._1 )
  }

  override def getID = 0

}
