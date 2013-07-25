package be.angelcorp.omicronai.gui.effects

import de.lessvoid.nifty.builder.EffectBuilder

/**
 * This is a custom EffectBuilder for move effect.
 */
class MoveEffectBuilder extends EffectBuilder("move") {

  /**
   * Possible values are "in" and "out"
   */
  def mode(mode: String) {
    effectParameter("mode", mode)
  }

  /**
   * Possible values are "top", "bottom", "left", "right"
   */
  def direction(direction: String) {
    effectParameter("direction", direction)
  }

}

