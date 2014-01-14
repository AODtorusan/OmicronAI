package be.angelcorp.omicronai.ai.lance

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.Nifty
import com.lyndir.omicron.api.GameListener
import com.lyndir.omicron.api.model.{Security, Game, Color, PlayerKey}
import com.lyndir.omicron.api.model.Color.Template._
import be.angelcorp.omicronai.Settings._
import be.angelcorp.omicronai.ai.AI
import be.angelcorp.omicronai.gui.{GuiInterface, AiGui}
import be.angelcorp.omicronai.gui.screens
import be.angelcorp.omicronai.gui.layerRender.GridRenderer

class LanceAi( playerId: Int, key: PlayerKey, name: String, color: Color ) extends AI( playerId, key, name, color, color ) {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  Security.authenticate(this, key)

  def this( builder: Game.Builder) =
    this( builder.nextPlayerID, new PlayerKey, settings.ai.name, RED.get )


  def gameListener: GameListener = new GameListener {
    
  }

  def buildGuiInterface(gui: AiGui, nifty: Nifty): GuiInterface = new GuiInterface {
    nifty.addScreen( screens.Introduction.name, screens.Introduction.screen(nifty, gui) )
    nifty.addScreen( screens.ui.UserInterface.name, screens.ui.lance.LanceUserInterface.screen(nifty, gui) )

    nifty.gotoScreen( screens.Introduction.name )

    activeLayers += new GridRenderer(LanceAi.this)
  }

}
