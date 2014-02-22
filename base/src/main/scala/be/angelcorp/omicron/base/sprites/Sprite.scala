package be.angelcorp.omicron.base.sprites

import org.newdawn.slick.Image

trait Sprite {

  def key:    String

  def image:  Image

  def dependencies: Iterable[Sprite]

}

