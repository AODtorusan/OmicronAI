package be.angelcorp.omicronai.gui.nifty

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import de.lessvoid.nifty.elements.Element
import de.lessvoid.nifty.elements.render.TextRenderer

trait ListBoxViewConverter[T] extends de.lessvoid.nifty.controls.ListBox.ListBoxViewConverter[T] {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )

  def stringify(item: T): String

  def display(element: Element, item: T) {
    element.getRenderer(classOf[TextRenderer]) match {
      case renderer: TextRenderer =>
        renderer.setText( stringify(item) )
      case any =>
        if (any != null) logger.info(any.toString)
        logger.warn("you're using the ListBoxViewConverterSimple but there is no TextRenderer on the listBoxElement." +
                    "You've probably changed the item template but did not provided your own " +
                    "ListBoxViewConverter to the ListBox.")
    }
  }

  def getWidth(element: Element, item: T): Int = {
    element.getRenderer(classOf[TextRenderer]) match {
      case renderer: TextRenderer =>
        val resultText = element.getNifty.specialValuesReplace(item.toString)
        element.getRenderer(classOf[TextRenderer]).getFont.getWidth(resultText)
      case any =>
        logger.warn("you're using the ListBoxViewConverterSimple but there is no TextRenderer on the listBoxElement." +
                    "You've probably changed the item template but did not provided your own " +
                    "ListBoxViewConverter to the ListBox.")
        0
    }
  }

}
