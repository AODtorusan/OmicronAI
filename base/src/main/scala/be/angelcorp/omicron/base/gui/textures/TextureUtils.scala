package be.angelcorp.omicron.base.gui.textures

import scala.List
import java.awt._
import java.awt.geom.Line2D
import java.awt.image.{ColorModel, BufferedImage}
import org.newdawn.slick.ImageBuffer
import be.angelcorp.omicron.base.HexTile

object TextureUtils {

  private val rgba = ColorModel.getRGBdefault

  def cutTile(img: BufferedImage, centerX: Int, centerY: Int, size: Int) = {
    val xPoints = Array( 0.00f * HexTile.width  * size + centerX,
                         0.50f * HexTile.width  * size + centerX,
                         0.50f * HexTile.width  * size + centerX,
                         0.00f * HexTile.width  * size + centerX,
                        -0.50f * HexTile.width  * size + centerX,
                        -0.50f * HexTile.width  * size + centerX ).map(_.toInt)
    val yPoints = Array( 0.50f * HexTile.height * size + centerY,
                         0.25f * HexTile.height * size + centerY,
                        -0.25f * HexTile.height * size + centerY,
                        -0.50f * HexTile.height * size + centerY,
                        -0.25f * HexTile.height * size + centerY,
                         0.25f * HexTile.height * size + centerY ).map(_.toInt)
    val tile = new Polygon(xPoints, yPoints, xPoints.size)

    val roi = new Rectangle(centerX - size/2, centerY - size/2, size, size)
    transform(img, roi) ( (row, col, pix) => {
      pix(3) = if (tile.contains(row, col)) 255 else 0
    } )
  }

  def cutTile(img: BufferedImage, centerX: Int, centerY: Int, size: Int, blendradius: Int) = {
    val tsize = size - blendradius / 2 // Solid tile size
    val isize = size + blendradius / 2 // Resulting image size
    val xPoints = Array( 0.00f * HexTile.width  * tsize + centerX,
        0.50f * HexTile.width  * tsize + centerX,
        0.50f * HexTile.width  * tsize + centerX,
        0.00f * HexTile.width  * tsize + centerX,
        -0.50f * HexTile.width  * tsize + centerX,
        -0.50f * HexTile.width  * tsize + centerX ).map(_.toInt)
    val yPoints = Array( 0.50f * HexTile.height * tsize + centerY,
      0.25f * HexTile.height * tsize + centerY,
      -0.25f * HexTile.height * tsize + centerY,
      -0.50f * HexTile.height * tsize + centerY,
      -0.25f * HexTile.height * tsize + centerY,
      0.25f * HexTile.height * tsize + centerY ).map(_.toInt)

    val tile = new Polygon(xPoints, yPoints, xPoints.size)
    val points = ((xPoints zip yPoints) ++ List((xPoints(0), yPoints(0)))).map( pnt => new Point(pnt._1, pnt._2) )
    val lines  = points.sliding(2).toList
    def distance(p: Point) = lines.map( line => pointToLineDistance(line(0), line(1), p)*2 ).min

    val roi = new Rectangle(centerX - isize/2, centerY - isize/2, isize, isize)
    transform(img, roi) ( (row, col, pix) => {
      val p  = new Point(row, col)
      pix(3) = math.max(if (tile.contains(p)) 255 else math.round(easeInOutAtan(distance(p), blendradius)), 0).toInt
    } )
  }

  /**
   * @param img Image to transform
   * @param f Function that in-place modifies the RGBA value for each pixel (row, col, pixeldata)
   * @return A new transformed image
   */
  def transform( img: BufferedImage)( f: (Int, Int, Array[Int]) => Unit ) = {
    val inRaster  = img.getRaster
    val outRaster = rgba.createCompatibleWritableRaster(img.getWidth, img.getHeight)
    val pixel     = Array.fill[Int](4)(255)
    for (i <- 0 until inRaster.getWidth;
         j <- 0 until inRaster.getHeight){
      inRaster.getPixel(i, j, pixel)
      f(i, j, pixel)
      outRaster.setPixel(i, j, pixel)
    }
    new BufferedImage(rgba, outRaster, false, null)
  }

  /**
   * @param img Image to transform
   * @param f Function that in-place modifies the RGBA value for each pixel (rowInOriginal, colInOriginal, pixeldata)
   * @return A new transformed image
   */
  def transform( img: BufferedImage, roi: Rectangle)( f: (Int, Int, Array[Int]) => Unit ) = {
    val inRaster  = img.getRaster
    val outRaster = rgba.createCompatibleWritableRaster(roi.width, roi.height)

    val pixel = Array.fill[Int](4)(255)
    for(i <- 0 until roi.width;
        j <- 0 until roi.height) {
      inRaster.getPixel(i + roi.x, j + roi.y, pixel)
      f(i + roi.x, j + roi.y, pixel)
      outRaster.setPixel(i, j, pixel)
    }
    new BufferedImage(rgba, outRaster, false, null)
  }

  def pointToLineDistance(a2: Point, b2: Point, p2: Point) =
    Line2D.ptSegDist(a2.x, a2.y, b2.x, b2.y, p2.x, p2.y)

  def easeInOutAtan(d: Double, r: Double) = {
    val a = r/2
    val b = r/5 // Determines the slope at r/2 or midway
    ( 1 - math.tanh((d-a)/b) ) * 255.0 / 2.0
  }

  def toTexture(image: BufferedImage) = {

    val pixel  = Array.fill[Int](4)(255)
    val raster = image.getData
    val buffer = new ImageBuffer(image.getWidth, image.getHeight)

    for (i <- 0 until raster.getWidth;
         j <- 0 until raster.getHeight){
      raster.getPixel(i, j, pixel)
      buffer.setRGBA(i, j, pixel(0), pixel(1), pixel(2), pixel(3))
    }

    buffer.getImage
  }

}
