package be.angelcorp.omicronai.gui.slick

import scala.collection.mutable

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.StringTokenizer

import org.newdawn.slick.Image
import org.newdawn.slick.SlickException
import org.newdawn.slick.util.ResourceLoader

/**
 * A font implementation that will parse BMFont format font files. The font
 * files can be output by Hiero, which is included with Slick, and also the
 * AngelCode font tool available at:
 *
 * <a href="http://www.angelcode.com/products/bmfont/">http://www.angelcode.com/products/bmfont/</a>
 *
 * This implementation copes with both the font display and kerning information
 * allowing nicer looking paragraphs of text. Note that this utility only
 * supports the text BMFont format definition file.
 *
 * @author kevin
 * @author davedes various modifications
 * @author Nathan Sweet <misc@n4te.com>
 */
class SpriteFont(fontFile: InputStream, fontImage: Image, hint: Int) {

  /** The height of a line */
  var lineHeight: Int = _

  // now parse the font file
  val in     = new BufferedReader(new InputStreamReader(fontFile))
  val info   = in.readLine
  val common = in.readLine
  //baseline = parseMetric(common, "omicron.base="); //not used apparently
  val ascent  = parseMetric(common, "ascent=")
  val descent = parseMetric(common, "descent=")
  val leading = parseMetric(common, "leading=")

  val page = in.readLine

  val kerning  = mutable.Map[Int, mutable.ListBuffer[Int]]()
  val charDefs = List.newBuilder[Glyph]
  var maxChar = 0
  var done    = false
  while (!done) {
    val line = in.readLine
    if (line == null) {
      done = true
    } else {
      if (line.startsWith("chars c")) {
        // ignore
      } else if (line.startsWith("char")) {
        val c = parseChar(line)
        if (c != null) {
          maxChar = Math.max(maxChar, c.id)
          charDefs += c
        }
      }
      if (line.startsWith("kernings c")) {
        // ignore
      } else if (line.startsWith("kerning")) {
        val tokens = new StringTokenizer(line, " =")
        tokens.nextToken // kerning
        tokens.nextToken // first
        val first = tokens.nextToken.toInt // first
        // value
        tokens.nextToken // second
        val second = tokens.nextToken.toInt // second
        // value
        tokens.nextToken // offset
        val offset = tokens.nextToken.toInt // offset
        // value
        var values = kerning.getOrElseUpdate(first, mutable.ListBuffer[Int]() )
        // Pack the character and kerning offset into a short.
        values += ((offset << 8) | second)
      }
    }
  }

  /** The characters building up the font */
  var chars = Array.ofDim[Glyph](maxChar + 1)
  for (g <- charDefs.result())
    chars(g.id) = g

  // Turn each list of kerning values into a short[] and set on the chardef.
  for (entry <- kerning) {
    val first = entry._1
    val valueList = entry._2
    val valueArray = valueList.toArray
    chars(first).kerning = valueArray
  }

  //def this(fontFile: String, fontImage: Image) = this(fontFile, fontImage, SpriteFont.NO_HINT)

  //def this(fontFile: String, fontImage: Image, hint: Int) = this(ResourceLoader.getResourceAsStream(fontFile), fontImage, hint)

  //def this(fontFile: InputStream, fontImage: Image) = this(fontFile, fontImage, SpriteFont.NO_HINT)

  def parseMetric(str: String, sub: String) = str.indexOf(sub) match {
    case -1  => -1
    case idx =>
      val subStr = str.substring(idx + sub.length)
      val idx2 = subStr.indexOf(' ')
      subStr.substring(0, if (idx2 != -1) idx2 else subStr.length).toInt
  }



  def getHeight(text: CharSequence): Float = getHeight(text, 0, text.length)

  def getHeight(text: CharSequence, startIndex: Int, endIndex: Int): Float = {
    val heights = for (i <- 0 until text.length;
                       id = text.charAt(i) if !(id == '\n' || id == ' ');
                       g = getGlyph(id) if g != null) yield g.height + g.yoffset
    if (heights.isEmpty) 0 else heights.max
  }

  def getYOffset(text: CharSequence): Int = getYOffset(text, 0, text.length())

  def getYOffset(text: CharSequence, startIndex: Int, endIndex: Int): Int = {
    if (endIndex-startIndex==0)
      0
    else {
      var minYOffset = Integer.MAX_VALUE
      for (idx <- startIndex  until endIndex) {
        val id = text.charAt(idx)
        val g = if (id==' '||id=='\n') null else getGlyph(id)
        if (g != null)
          minYOffset = Math.min(g.yoffset, minYOffset)
      }
      minYOffset
    }
  }

  /**
   * Parse a single character line from the definition
   *
   * @param line
	 *            The line to be parsed
   * @return The character definition from the line
   * @throws SlickException
	 *             Indicates a given character is not valid in an angel code
   *             font
   */
  def parseChar(line: String): Glyph = {
    val tokens = new StringTokenizer(line, " =")

    tokens.nextToken; // char
    tokens.nextToken; // id
    tokens.nextToken().toInt match { // id value
      case id if id < 0 => null
      case id if id > SpriteFont.MAX_CHAR =>
        throw new SlickException(s"Invalid character '$id': SpriteFont does not support characters above ${SpriteFont.MAX_CHAR}")
      case id =>
        tokens.nextToken(); // x
        val x = tokens.nextToken.toInt // x value
        tokens.nextToken(); // y
        val y = tokens.nextToken.toInt // y value
        tokens.nextToken(); // width
        val width = tokens.nextToken.toInt // width value
        tokens.nextToken(); // height
        val height = tokens.nextToken.toInt // height value
        tokens.nextToken(); // x offset
        val xoffset = tokens.nextToken.toInt // xoffset value
        tokens.nextToken(); // y offset
        val yoffset = tokens.nextToken.toInt // yoffset value
        tokens.nextToken(); // xadvance
        val xadvance = tokens.nextToken.toInt // xadvance

        if (id != ' ') {
          lineHeight = Math.max(height + yoffset, lineHeight)
        }
        val img = fontImage.getSubImage(x, y, width, height)
        new Glyph(id, x, y, width, height, xoffset, yoffset, xadvance, img)
    }
  }

  /**
   * Returns the character definition for the given character.
   *
   * @param c the desired character
   * @return the CharDef with glyph info
   */
  def getGlyph(c: Char): Glyph = {
    val g = c match {
      case 0 => null
      case c if c >= chars.length => null
      case c => chars(c)
    }
    if (g != null)
      g
    else {
      val c2 = if (g == null && hint == SpriteFont.CASE_INSENSITIVE) {
        if (c >= 65 && c <= 90)
          c + 32
        else if (c >= 97 && c <= 122)
          c - 32
        else
          c
      } else c
      c2 match {
        case 0 => null
        case c if c >= chars.length => null
        case c => chars(c)
      }
    }
  }

}

/**
 * The definition of a single character as defined in the AngelCode file format
 *
 * @param id The id of the character
 * @param x The x location on the sprite sheet
 * @param y The y location on the sprite sheet
 * @param width The width of the character image
 * @param height The height of the character image
 * @param xoffset The amount the x position should be offset when drawing the image
 * @param yoffset The amount the y position should be offset when drawing the image
 * @param xadvance The amount to move the current position after drawing the character
 * @param image The sub image; will be null if fontImage was null at creation time.
 *
 * @author kevin
 */
class Glyph(val id: Int, val x: Int, val y: Int, val width: Int, val height: Int, val xoffset: Int, val yoffset: Int, val xadvance: Int, val image: Image) {

  /** The kerning info for this character */
  var kerning: Array[Int] = _

  override def toString = s"[CharDef id=$id x=$x y=$y]"

  /**
   * Get the kerning offset between this character and the specified character.
   *
   * @param otherCodePoint The other code point
   * @return the kerning offset
   */
  def getKerning(otherCodePoint: Int): Int = {
    if (kerning == null)
      0
    else {
      var low  = 0
      var high = kerning.length - 1
      while (low <= high) {
        val midIndex = (low + high) >>> 1;
        val value = kerning(midIndex)
        val foundCodePoint = value & 0xff
        if (foundCodePoint < otherCodePoint)
          low = midIndex + 1
        else if (foundCodePoint > otherCodePoint)
          high = midIndex - 1
        else
          return value >> 8
      }
      0
    }
  }
}

object SpriteFont {
  /** The highest character that BitmapFont will support. */
  val MAX_CHAR = 255
  val NO_HINT  = 0
  val CASE_INSENSITIVE = 1
}
