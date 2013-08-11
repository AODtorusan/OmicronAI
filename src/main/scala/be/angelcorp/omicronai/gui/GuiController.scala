package be.angelcorp.omicronai.gui

import de.lessvoid.nifty.controls.NiftyControl

trait GuiController {

  def populate() { }

  def updateUI() { }

  def disable(control: NiftyControl) {
    control.setEnabled(false)
    control.setFocusable(false)
  }
  def enable(control: NiftyControl) {
    control.setEnabled(true)
    control.setFocusable(true)
  }

}
