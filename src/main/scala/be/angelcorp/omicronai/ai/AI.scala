package be.angelcorp.omicronai.ai

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.ActorRef
import akka.pattern.ask
import de.lessvoid.nifty.Nifty
import com.lyndir.omicron.api.model._
import be.angelcorp.omicronai.gui.{AiGuiOverlay, AiGui, GuiInterface}
import be.angelcorp.omicronai.world.ReloadReady
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.world.ReloadReady
import scala.Some
import be.angelcorp.omicronai.Location

abstract class AI( playerId: Int, key: PlayerKey, name: String, primaryColor: Color, secondaryColor: Color  ) extends Player( playerId, key, name, primaryColor, secondaryColor) {

  def buildGuiInterface(gui: AiGuiOverlay, nifty: Nifty): GuiInterface

  def start() {
    Security.authenticate(this, key)
    getController.getGameController.setReady()
  }

}
