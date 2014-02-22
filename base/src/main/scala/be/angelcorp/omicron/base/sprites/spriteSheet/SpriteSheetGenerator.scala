package be.angelcorp.omicron.base.sprites.spriteSheet

import java.awt.Rectangle
import scala.math._
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import be.angelcorp.omicron.base.sprites.BufferedSprite
import be.angelcorp.omicron.base.sprites.spriteSheet.MaxRectsBinPack.OutOfSpace

class SpriteSheetGenerator( val sprites: Iterable[BufferedSprite] ) {
  private val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def defaultSide() = {
    val area       = sprites.foldLeft(0)( (area, i) => area + i.bufferedSprite.getWidth * i.bufferedSprite.getHeight )
    val areaSide   = sqrt( area )
    val widthSize  = sprites.maxBy( _.bufferedSprite.getWidth  ).bufferedSprite.getWidth
    val heightSize = sprites.maxBy( _.bufferedSprite.getHeight ).bufferedSprite.getHeight
    val squareSize = get2Fold( math.max(math.max(areaSide, widthSize), heightSize) )
    if ((squareSize * squareSize) / area >= 2 )
      (squareSize, squareSize/2)
    else
      (squareSize, squareSize)
  }

  def pack() = {
    val sides = defaultSide()
    packInto(sides._1, sides._2)
  }

  def packInto( width: Int, height: Int ): SpriteSheetSource = {
    val heuristic = RectBestShortSideFit
    val packer = new MaxRectsBinPack(width, height)
    try {
      val mapping = packer.insert( sprites.toSeq, heuristic )
      new SpriteSheetSource(width, height, mapping.mapValues( r => new Rectangle(r.x, r.y, r.width, r.height) ))
    } catch {
      case e: OutOfSpace =>
        logger.error("Failed! Could not find a proper position to pack this rectangle into. Growing the target image.")
        packInto(width * 2, height)
    }
  }

  /**
    * Get the closest greater power of 2 to the fold number
    *
    * @param fold The target number
    * @return The power of 2
    */
  def get2Fold(fold: Double) = {
    var ret = 2
    while (ret < fold) ret *= 2
    ret
  }

}
