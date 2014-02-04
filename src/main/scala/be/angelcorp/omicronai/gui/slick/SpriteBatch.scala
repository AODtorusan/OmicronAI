package be.angelcorp.omicronai.gui.slick

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GLContext
import org.newdawn.slick.Color
import org.newdawn.slick.Image
import org.newdawn.slick.opengl.Texture
import org.newdawn.slick.util.FastTrig

/**
 *
 * @param maxVerts
 *    Returns the size of this ImageBatch as given in construction (default 1000).
 *    The internal array will have a capacity of size * 8.
 *
 *    A large internal array will require less calls to render(), but will take up more memory. For example,
 *    an ImageBatch with a size of 6 would be ideal if we are only rendering a single image (made up of tris)
 *    within begin/end (six vertices, 8 bytes per vertex -- 2 for XY, 2 for texture UV, 4 for RGBA).
 *
 *    However, it's usually better to create a single large-size ImageBatch instance and re-use it throughout your game.
 *
 * @return how many vertices to expect
 */
class SpriteBatch(maxVerts: Int = 1000) {

  val TOLERANCE = 48 //we assume triangles is in use...

  var idx: Int         = 0
  var texture: Texture = _
  var renderCalls: Int = 0

  var currentColor = Color.white

  var vboID = 0

  if (maxVerts<=0) throw new IllegalArgumentException("batch size must be larger than 0")
  val len = maxVerts * 8
  val vertices = BufferUtils.createFloatBuffer(len)
  val colors = BufferUtils.createFloatBuffer(len)
  val texcoords = BufferUtils.createFloatBuffer(len)
  GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY)
  GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
  GL11.glEnableClientState(GL11.GL_COLOR_ARRAY)

  def flush() {
    if (idx > 0) render()
    idx = 0
    texture = null
    vertices.clear()
    texcoords.clear()
    colors.clear()
  }

  /**
   * Sends vertex, color and UV data to the GPU.
   */
  def render(): Unit = if (idx > 0) {
    renderCalls += 1
    //bind the last texture
    if (texture!=null)
      texture.bind()
    vertices.flip()
    colors.flip()
    texcoords.flip()

    GL11.glVertexPointer(2, 0, vertices)
    GL11.glColorPointer(4, 0, colors)
    GL11.glTexCoordPointer(2, 0, texcoords)

    GL11.glDrawArrays(SpriteBatch.mode, 0, idx)
    vertices.clear()
    colors.clear()
    texcoords.clear()
    idx = 0
  }
  //
  //	public void drawText(SpriteFont defaultFont, StyledText text, float x, float y) {
  //		SpriteFont.Glyph lastDef = null;
  //		SpriteFont lastFont = null;
  //		Color old = currentColor;
  //
  //		float maxLineHeight = defaultFont.getLineHeight();
  //		float minY = text.getGroupCount()>0 ? Integer.MAX_VALUE : 0;
  //		float maxBaseline = 0;
  //		for (int gc=0; gc<text.getGroupCount(); gc++) {
  //			StyledText.Group g = text.getGroup(gc);
  //			if (g.getFont()!=null) {
  //				maxLineHeight = Math.max(maxLineHeight, g.getFont().getLineHeight());
  //				minY = Math.min(minY, g.getYOffset());
  //				maxBaseline = Math.max(maxBaseline, g.getFont().getAscent());
  //			} else {
  //				minY = Math.min(minY, defaultFont.getYOffset(g.getText()));
  //				maxBaseline = Math.max(maxBaseline, defaultFont.getAscent());
  //			}
  //
  //		}
  //
  //		for (int gc=0; gc<text.getGroupCount(); gc++) {
  //			StyledText.Group g = text.getGroup(gc);
  //			SpriteFont newFont = g.getFont()!=null ? g.getFont() : defaultFont;
  //			Color newColor = g.getColor()!=null ? g.getColor() : old;
  //			CharSequence newStr = g.getText();
  //			//TODO: clean up this method
  //			float minYOff = g.getFont()==null ? defaultFont.getYOffset(newStr) : g.getYOffset();
  //			float height = g.getFont()==null ? defaultFont.getHeight(newStr) : g.getHeight();
  //			float baseline = g.getFont()==null ? defaultFont.getAscent() : g.getFont().getAscent();
  //			float descent = g.getFont()==null ? defaultFont.getDescent() : g.getFont().getDescent();
  //			float yoff = maxBaseline - baseline;
  //
  //			if (newFont!=lastFont) { //reset the last glyph
  //				lastDef = null;
  //			}
  //
  //			for (int i=0; i<newStr.length(); i++) {
  //				char c = newStr.charAt(i);
  //				SpriteFont.Glyph def = newFont.getGlyph(c);
  //				if (def==null)
  //					continue;
  //				if (lastDef!=null)
  //					x += lastDef.getKerning(c);
  //				lastDef = def;
  //				setColor(newColor);
  //				drawImage(def.image, x + def.xoffset, y + def.yoffset + yoff - minY);
  //				x += def.xadvance;
  //			}
  //		}
  //		setColor(old);
  //	}

  def drawTextImpl(font: SpriteFont, text: CharSequence, x0: Float, y0: Float, startIndex: Int, endIndex: Int, multiLine: Boolean) {
    var lastDef: Glyph = null

    var startX = x0
    var x = x0
    var y = y0
    for (i <- startIndex until endIndex) {
      val c = text.charAt(i)
      if (multiLine && c=='\n') {
        y += font.lineHeight
        x = startX
      }
      font.getGlyph(c) match {
        case null =>
        case g =>
          if (lastDef!=null) x += lastDef.getKerning(c)
          lastDef = g
          drawImage(g.image, x + g.xoffset, y + g.yoffset)
          x += g.xadvance
      }
    }
  }

  def drawTextMultiLine(font: SpriteFont, text: CharSequence, x: Float, y: Float) {
    drawTextImpl(font, text, x, y, 0, text.length(), multiLine = true)
  }

  def drawTextMultiLine(font: SpriteFont, text: CharSequence, x: Float, y: Float, startIndex: Int, endIndex: Int) {
    drawTextImpl(font, text, x, y, startIndex, endIndex, multiLine = true)
  }

  def drawText(font: SpriteFont, text: CharSequence, x: Float, y: Float) {
    drawTextImpl(font, text, x, y, 0, text.length(), multiLine = false)
  }

  def drawText(font: SpriteFont, text: CharSequence, x: Float, y: Float, startIndex: Int, endIndex: Int) {
    drawTextImpl(font, text, x, y, startIndex, endIndex, multiLine = false)
  }

  def drawImageScaled(image: Image, x: Float, y: Float, scale: Float) {
    drawImage(image, x, y, image.getWidth*scale, image.getHeight*scale)
  }

  def drawImage(image: Image) {
    drawImage(image, 0, 0)
  }

  def drawImage(image: Image, x: Float, y: Float) {
    drawImage(image, x, y, null)
  }

  def drawImage(image: Image, x: Float, y: Float, corners: Array[Color]) {
    drawImage(image, x, y, image.getWidth, image.getHeight, corners)
  }

  def drawImage(image: Image, x: Float, y: Float, w: Float, h: Float) {
    drawImage(image, x, y, w, h, null)
  }

  def drawImage(image: Image, x: Float, y: Float, rotation: Float) {
    drawImage(image, x, y, rotation, image.getWidth, image.getHeight, null)
  }

  def drawImage(image: Image, x: Float, y: Float, rotation: Float, w: Float, h: Float, corners: Array[Color]) {
    if (rotation==0) {
      drawImage(image, x, y, w, h, corners)
      return
    }
    checkRender(image)

    val scaleX = w/image.getWidth
    val scaleY = h/image.getHeight

    val cx = image.getCenterOfRotationX*scaleX
    val cy = image.getCenterOfRotationY*scaleY

    val p1x = -cx
    val p1y = -cy
    val p2x = w - cx
    val p2y = -cy
    val p3x = w - cx
    val p3y = h - cy
    val p4x = -cx
    val p4y = h - cy

    val rad = Math.toRadians(rotation)
    val cos = FastTrig.cos(rad).toFloat
    val sin = FastTrig.sin(rad).toFloat

    val tx = image.getTextureOffsetX
    val ty = image.getTextureOffsetY
    val tw = image.getTextureWidth
    val th = image.getTextureHeight

    val x1 = (cos * p1x - sin * p1y) + cx // TOP LEFT
    val y1 = (sin * p1x + cos * p1y) + cy
    val x2 = (cos * p2x - sin * p2y) + cx // TOP RIGHT
    val y2 = (sin * p2x + cos * p2y) + cy
    val x3 = (cos * p3x - sin * p3y) + cx // BOTTOM RIGHT
    val y3 = (sin * p3x + cos * p3y) + cy
    val x4 = (cos * p4x - sin * p4y) + cx // BOTTOM LEFT
    val y4 = (sin * p4x + cos * p4y) + cy
    drawQuadElement(
      x+x1, y+y1, tx,    ty,    if (corners!=null) corners(0) else null,
      x+x2, y+y2, tx+tw, ty,    if (corners!=null) corners(1) else null,
      x+x3, y+y3, tx+tw, ty+th, if (corners!=null) corners(2) else null,
      x+x4, y+y4, tx,    ty+th, if (corners!=null) corners(3) else null
    )
  }

  def drawImage(image: Image, x: Float, y: Float, w: Float, h: Float, corners: Array[Color]) {
    checkRender(image)
    val tx = image.getTextureOffsetX
    val ty = image.getTextureOffsetY
    val tw = image.getTextureWidth
    val th = image.getTextureHeight
    drawImage(image, x, y, w, h, tx, ty, tw, th, corners)
    //		drawQuadElement(x, y, tx, ty, corners!=null ? corners[0] : null,
    //				 		x+w, y, tx+tw, ty, corners!=null ? corners[1] : null,
    //				 		x+w, y+h, tx+tw, ty+th, corners!=null ? corners[2] : null,
    //				 		x, y+h, tx, ty+th, corners!=null ? corners[3] : null);
  }

  def drawSubImage(image: Image, srcx: Float, srcy: Float, srcwidth: Float, srcheight: Float, x: Float, y: Float) {
    drawSubImage(image, srcx, srcy, srcwidth, srcheight, x, y, srcwidth, srcheight)
  }

  def drawSubImage(image: Image, srcx: Float, srcy: Float, srcwidth: Float, srcheight: Float, x: Float, y: Float, w: Float, h: Float) {
    drawSubImage(image, srcx, srcy, srcwidth, srcheight, x, y, w, h, null)
  }

  def drawSubImage(image: Image, srcx: Float, srcy: Float, srcwidth: Float, srcheight: Float, x: Float, y: Float, w: Float, h: Float, corners: Array[Color]) {
    checkRender(image)

    val iw = image.getWidth
    val ih = image.getHeight
    val tx = (srcx / iw * image.getTextureWidth) + image.getTextureOffsetX
    val ty = (srcy / ih * image.getTextureHeight) + image.getTextureOffsetY
    val tw = w / iw * image.getTextureWidth
    val th = h / ih * image.getTextureHeight
    drawQuadElement(
      x,     y,     tx,      ty,      if (corners != null) corners(0) else null,
      x + w, y,     tx + tw, ty,      if (corners != null) corners(1) else null,
      x + w, y + h, tx + tw, ty + th, if (corners != null) corners(2) else null,
      x,     y + h, tx,      ty + th, if (corners != null) corners(3) else null
    )
  }

  def drawImage(image: Image, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, uWidth: Float, vHeight: Float, corners: Array[Color]) {
    checkRender(image)
    drawQuadElement(
      x,       y,        u,        v,         if(corners!=null) corners(0) else null,
      x+width, y,        u+uWidth, v,         if(corners!=null) corners(1) else null,
      x+width, y+height, u+uWidth, v+vHeight, if(corners!=null) corners(2) else null,
      x,       y+height, u,        v+vHeight, if(corners!=null) corners(3) else null
    )
  }

  /**
   * @param texcoords a texcoord for each vertex (8 elements)
   */
  def drawImage(image: Image, x: Float, y: Float, points: Array[Float], texcoords: Array[Float], offset: Int, texcoordsOffset: Int, corners: Array[Color]) {
    checkRender(image)

    val x1 = points(offset+0)
    val y1 = points(offset+1)
    val x2 = points(offset+2)
    val y2 = points(offset+3)
    val x3 = points(offset+4)
    val y3 = points(offset+5)
    val x4 = points(offset+6)
    val y4 = points(offset+7)

    val u1 = texcoords(texcoordsOffset+0)
    val v1 = texcoords(texcoordsOffset+1)
    val u2 = texcoords(texcoordsOffset+2)
    val v2 = texcoords(texcoordsOffset+3)
    val u3 = texcoords(texcoordsOffset+4)
    val v3 = texcoords(texcoordsOffset+5)
    val u4 = texcoords(texcoordsOffset+6)
    val v4 = texcoords(texcoordsOffset+7)
    drawQuadElement(
      x+x1, y+y1, u1, v1, if(corners!=null) corners(0) else null,
      x+x2, y+y2, u2, v2, if(corners!=null) corners(1) else null,
      x+x3, y+y3, u3, v3, if(corners!=null) corners(2) else null,
      x+x4, y+y4, u4, v4, if(corners!=null) corners(3) else null
    )
  }

  def checkRender(image: Image) {
    if (image == null || image.getTexture == null)
      throw new NullPointerException("null texture")

    //we need to bind a different texture. this is for convenience;
    // ideally the user should order their rendering wisely to minimize texture binds
    if (image.getTexture != texture) {
      //apply the last texture
      render()
      texture = image.getTexture
    } else if (idx >= maxVerts - TOLERANCE)
      render()
  }

  /**
   * Specifies vertex data.
   *
   * @param x the x position
   * @param y the y position
   * @param u the U texcoord
   * @param v the V texcoord
   * @param color the color for this vertex
   */
  protected def vertex(x: Float, y: Float, u: Float, v: Float, color: Color) {
    vertices.put(x)
    vertices.put(y)
    texcoords.put(u)
    texcoords.put(v)
    val c = if(color!=null) color else currentColor
    colors.put(c.r)
    colors.put(c.g)
    colors.put(c.b)
    colors.put(c.a)
    idx += 1
  }

  /**
   * Draws a quad-like element using either GL_QUADS or GL_TRIANGLES, depending
   * on this batch's configuration.
   */
  protected def drawQuadElement( x1: Float, y1: Float, u1: Float, v1: Float, c1: Color,   //TOP LEFT
                                 x2: Float, y2: Float, u2: Float, v2: Float, c2: Color,   //TOP RIGHT
                                 x3: Float, y3: Float, u3: Float, v3: Float, c3: Color,   //BOTTOM RIGHT
                                 x4: Float, y4: Float, u4: Float, v4: Float, c4: Color) { //BOTTOM LEFT
    if (SpriteBatch.mode == GL11.GL_TRIANGLES) {
      //top left, top right, bottom left
      vertex(x1, y1, u1, v1, c1)
      vertex(x2, y2, u2, v2, c2)
      vertex(x4, y4, u4, v4, c4)
      //top right, bottom right, bottom left
      vertex(x2, y2, u2, v2, c2)
      vertex(x3, y3, u3, v3, c3)
      vertex(x4, y4, u4, v4, c4)
    } else {
      //quads: top left, top right, bottom right, bottom left
      vertex(x1, y1, u1, v1, c1)
      vertex(x2, y2, u2, v2, c2)
      vertex(x3, y3, u3, v3, c3)
      vertex(x4, y4, u4, v4, c4)
    }
  }
}

object SpriteBatch {

  val STRATEGY_VERTEX_ARRAYS = 2
  val STRATEGY_VBO           = 4
  val STRATEGY_VBO_MAPPED    = 8
  val STRATEGY_SHADER        = 16
  val STRATEGY_BEST          = 32 //tries to use shader, then VBO mapped something like new SpriteBatch

  def isVBOSupported = GLContext.getCapabilities.GL_ARB_vertex_buffer_object

  val ALIGN_LEFT    = 0
  val ALIGN_CENTER  = 1
  val ALIGN_RIGHT   = 2

  var mode = GL11.GL_TRIANGLES

  /**
   * Whether to send the image data as GL_TRIANGLES
   * or GL_QUADS. By default, GL_TRIANGLES is used.
   *
   * @param b true to use triangle rendering
   */
  def useTriangles(b: Boolean) {
    mode = if (b) GL11.GL_TRIANGLES else GL11.GL_QUADS
  }

  /**
   * Returns whether to send the image data as GL_TRIANGLES
   * or GL_QUADS. By default, GL_TRIANGLES is used.
   *
   * @return true if we are using triangle rendering
   */
  def isUseTriangles = mode == GL11.GL_TRIANGLES

}
