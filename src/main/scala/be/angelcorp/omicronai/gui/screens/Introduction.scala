package be.angelcorp.omicronai.gui.screens

import be.angelcorp.omicronai.gui.NiftyConstants._
import be.angelcorp.omicronai.gui.effects.FadeEffectBuilder
import de.lessvoid.nifty.builder.{LayerBuilder, ScreenBuilder}
import de.lessvoid.nifty.screen.DefaultScreenController
import de.lessvoid.nifty.Nifty
import be.angelcorp.omicronai.gui.AiGui

object Introduction extends GuiScreen {

  val name = "introductionScreen"

  def screen(nifty: Nifty, gui: AiGui) = new ScreenBuilder( name ) {{
    controller(new IntroductionScreenController())

    inputMapping("de.lessvoid.nifty.input.mapping.DefaultScreenMapping")

    layer(new LayerBuilder("background") {{

      backgroundColor( black )

      onStartScreenEffect(new FadeEffectBuilder() {{
        startColor( black )
        endColor( transparent )
        length(2000)
        //inherit(true)
        //post(false)
      }})

    }})

  }}.build(nifty)

}

/**
 * Controller for the introduction screen.
 */
class IntroductionScreenController extends DefaultScreenController {

  override def onStartScreen() { gotoScreen( MainMenu.name ) }

}
