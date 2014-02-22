package be.angelcorp.omicron.base.sprites

import java.awt.image._
import org.newdawn.slick.{Image, ImageBuffer}
import org.newdawn.slick.opengl.TextureImpl
import org.newdawn.slick.opengl.ImageData.Format._

object SpriteUtils {

  def toImage( image: BufferedImage ): Image = {
    val pixel  = Array.fill[Int](4)(255)
    val raster = image.getData // Stored as RGB/RGBA
    val buffer = new ImageBuffer(image.getWidth, image.getHeight)

    for (i <- 0 until raster.getWidth;
         j <- 0 until raster.getHeight){
      raster.getPixel(i, j, pixel)
      buffer.setRGBA(i, j, pixel(0), pixel(1), pixel(2), pixel(3))
    }

    buffer.getImage
  }

  def toBufferedImage( image: Image ): BufferedImage = {
    val texture = image.getTexture.asInstanceOf[TextureImpl]
    val data    = texture.getTextureData
    def toInt(r: Int, g: Int, b: Int, a: Int = 255) = ((r & 0xff) << 24) | ((g & 0xff) << 16) | ((b & 0xff) << 8) | (a & 0xff)
    val pixels  = texture.getImageFormat match {
      case RGB  => data.sliding(3, 3).map {
        case Array(r, g, b) => toInt(r, g, b)
      }.toArray
      case BGRA => data.sliding(4, 4).map {
        case Array(b, g, r, a) => toInt(r, g, b, a)
      }.toArray
      case RGBA  => data.sliding(4, 4).map {
        case Array(r, g, b, a) => toInt(r, g, b, a)
      }.toArray
      case ALPHA => data.map {
        case a => toInt(0, 0, 0, a)
      }
      case GRAY  => data.map {
        case g => toInt(g, g, g)
      }
      case _ => ???
    }
    val buffer     = new DataBufferInt( pixels, pixels.size )
    val colorModel = new DirectColorModel(32, 0xff000000, 0x00ff0000, 0x0000ff00, 0x000000ff)
    val raster     = Raster.createPackedRaster(buffer, texture.getImageWidth, texture.getImageHeight, texture.getImageWidth, colorModel.getMasks, null)
    new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, null)
  }

}
