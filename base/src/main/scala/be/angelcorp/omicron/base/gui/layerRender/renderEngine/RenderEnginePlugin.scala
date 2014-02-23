package be.angelcorp.omicron.base.gui.layerRender.renderEngine

import org.newdawn.slick.Graphics

trait RenderEnginePlugin {

  def preSpriteLayerRender(  g: Graphics, spriteLayerId: Int ) {}
  def postSpriteLayerRender( g: Graphics, spriteLayerId: Int ) {}

}

