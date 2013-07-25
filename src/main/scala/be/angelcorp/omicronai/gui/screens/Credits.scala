package be.angelcorp.omicronai.gui.screens

import de.lessvoid.nifty.Nifty
import de.lessvoid.nifty.builder.{TextBuilder, PanelBuilder, LayerBuilder, ScreenBuilder}
import de.lessvoid.nifty.screen.DefaultScreenController
import be.angelcorp.omicronai.gui.effects.FadeEffectBuilder
import be.angelcorp.omicronai.gui.NiftyConstants._
import be.angelcorp.omicronai.gui.AiGui

object Credits extends GuiScreen {

  val name = "creditsScreen"

  def screen(nifty: Nifty, gui: AiGui) = new ScreenBuilder( name ) {{

    controller(new CreditsScreenController())

    inputMapping("de.lessvoid.nifty.input.mapping.DefaultScreenMapping")

    layer(new LayerBuilder("content") {{

      backgroundColor("#0000")
      childLayoutVertical()

      onStartScreenEffect(new FadeEffectBuilder() {{
        startColor("#fff0")
        endColor("#ffff")
        length(1000)
        startDelay(0)
        inherit(true)
        post(false)
      }})

      onEndScreenEffect(new FadeEffectBuilder() {{
        startColor("#ffff")
        endColor("#0000")
        length(500)
        startDelay(0)
        inherit(true)
        post(false)
      }})

      interactOnClick("back()")

      panel(new PanelBuilder("top") {{
        backgroundColor("#f006")

        height(pixels(100))
        childLayoutCenter()
        valignCenter()
        alignCenter()

        text(new TextBuilder() {{
          text("Credits")
          font( defaultFont )
          color("#000f")
          width("*")
          alignCenter()
          valignCenter()

          padding(pixels(50))
        }})

      }})

      panel(new PanelBuilder("middle") {{
        backgroundColor("#0f06")

        childLayoutCenter()
        width(percentage(80))
        height("*")

        alignCenter()
        valignCenter()

        text(new TextBuilder() {{
          text("Lead Programmer........................................Super Simon\n"
             + "Lead Designer..........................................Super Simon\n")
          font( defaultFont )
          color("#000f")
          width("*")
          alignCenter()
          valignCenter()

          padding(pixels(50))
        }})

      }})

      panel(new PanelBuilder("bottom") {{
        backgroundColor("#00f6")
        height(pixels(100))
      }})

    }})

  }}.build(nifty)

}

/**
 * Controller for the credits screen.
 */
class CreditsScreenController extends DefaultScreenController {

  def back() { gotoScreen( MainMenu.name ) }

}

