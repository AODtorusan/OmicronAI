package be.angelcorp.omicronai.gui.nifty.effects

import de.lessvoid.nifty.builder.EffectBuilder

/**
 * This is another custom EffectBuilder, in this case for the fade effect.
 * This two custom effect builders could be moved to nifty itself.
 */
class FadeEffectBuilder extends EffectBuilder("fade") {

  def startColor(startColor: String) {
    attributes.setAttribute("startColor", startColor)
  }

  def endColor(endColor: String) {
    attributes.setAttribute("endColor", endColor)
  }

}