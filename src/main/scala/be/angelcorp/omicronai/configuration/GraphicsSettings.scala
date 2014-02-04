package be.angelcorp.omicronai.configuration

import com.typesafe.config.Config

class GraphicsSettings( val width: Int,
                        val height: Int,
                        val fullscreen: Boolean,
                        val alwaysRender: Boolean,
                        val multisampling: Int,
                        val showFPS: Boolean,
                        val targetFrameRate: Int,
                        val vSync: Boolean )

object GraphicsSettings {
  def apply(c: Config) = {
    val width           = c.getInt("width")
    val height          = c.getInt("height")
    val fullscreen      = c.getBoolean("fullscreen")
    val alwaysRender    = c.getBoolean("alwaysRender")
    val multisampling   = c.getInt("multisampling")
    val showFPS         = c.getBoolean("showFPS")
    val targetFrameRate = c.getInt("targetFrameRate")
    val vSync           = c.getBoolean("vSync")
    new GraphicsSettings( width, height, fullscreen, alwaysRender, multisampling, showFPS, targetFrameRate, vSync )
  }
}
