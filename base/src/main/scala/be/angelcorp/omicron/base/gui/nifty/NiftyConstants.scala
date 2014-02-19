package be.angelcorp.omicron.base.gui.nifty

object NiftyConstants {

  def white(  alpha: Int ): String = "#%02x%02x%02x%02x".format(255, 255, 255, alpha)
  val white: String                = white(255)
  def silver( alpha: Int ): String = "#%02x%02x%02x%02x".format(191, 191, 191, alpha)
  val silver: String               = silver(255)
  def gray(   alpha: Int ): String = "#%02x%02x%02x%02x".format(128, 128, 128, alpha)
  val gray: String                 = gray(255)
  def black(  alpha: Int ): String = "#%02x%02x%02x%02x".format(  0,   0,   0, alpha)
  val black: String                = black(255)
  def red(    alpha: Int ): String = "#%02x%02x%02x%02x".format(255,   0,   0, alpha)
  val red: String                  = red(255)
  def maroon( alpha: Int ): String = "#%02x%02x%02x%02x".format(128,   0,   0, alpha)
  val maroon: String               = maroon(255)
  def yellow( alpha: Int ): String = "#%02x%02x%02x%02x".format(255, 255,   0, alpha)
  val yellow: String               = yellow(255)
  def olive(  alpha: Int ): String = "#%02x%02x%02x%02x".format(128, 128,   0, alpha)
  val olive: String                = olive(255)
  def lime(   alpha: Int ): String = "#%02x%02x%02x%02x".format(  0, 255,   0, alpha)
  val lime: String                 = lime(255)
  def green(  alpha: Int ): String = "#%02x%02x%02x%02x".format(  0, 128,   0, alpha)
  val green: String                = green(255)
  def aqua(   alpha: Int ): String = "#%02x%02x%02x%02x".format(  0, 255, 255, alpha)
  val aqua: String                 = aqua(255)
  def teal(   alpha: Int ): String = "#%02x%02x%02x%02x".format(  0, 128, 128, alpha)
  val teal: String                 = teal(255)
  def blue(   alpha: Int ): String = "#%02x%02x%02x%02x".format(  0,   0, 255, alpha)
  val blue: String                 = blue(255)
  def navy(   alpha: Int ): String = "#%02x%02x%02x%02x".format(  0,   0, 128, alpha)
  val navy: String                 = navy(255)
  def fuchsia(alpha: Int ): String = "#%02x%02x%02x%02x".format(255,   0, 255, alpha)
  val fuchsia: String              = fuchsia(255)
  def purple( alpha: Int ): String = "#%02x%02x%02x%02x".format(128,   0, 128, alpha)
  val purple: String               = purple(255)
  val transparent: String          = white(0)

  val defaultFont     = "ptsans.fnt"

  val directionTop    = "top"
  val directionBottom = "bottom"
  val directionLeft   = "left"
  val directionRight  = "right"

  val inMode  = "in"
  val outMode = "out"

}
