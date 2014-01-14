package be.angelcorp.omicronai.gui.input

trait InputHandler {

  /**
   * Process the given event.
   * @param event the event to process.
   * @return true, when the event has been processed and false, if not.
   */
  def handleInputEvent(event: GuiInputEvent): Boolean

}
