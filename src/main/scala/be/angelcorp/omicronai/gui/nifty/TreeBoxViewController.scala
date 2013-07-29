package be.angelcorp.omicronai.gui.nifty

import de.lessvoid.nifty.builder.ElementBuilder
import de.lessvoid.nifty.controls.{TreeItem, Label, ListBox}
import de.lessvoid.nifty.controls.label.builder.LabelBuilder
import de.lessvoid.nifty.elements.Element
import de.lessvoid.nifty.elements.render.TextRenderer
import de.lessvoid.nifty.spi.render.RenderFont
import de.lessvoid.nifty.tools.SizeValue

trait TreeBoxViewController[T] extends ListBox.ListBoxViewConverter[TreeItem[T]] {

  def stringify(item: T): String

  def display(element: Element, item: TreeItem[T]) {
    val spacer = element.findElementById("#tree-item-spacer")
    spacer.setConstraintWidth(SizeValue.px(item.getIndent))
    spacer.setConstraintHeight(SizeValue.px(1))
    spacer.setVisible(item.getIndent > 0)

    /*
    val icon = element.findElementById("#tree-item-icon")
    if (item.isLeaf)
      icon.setStyle(element.getStyle + "#leaf")
    else if (item.isExpanded)
      icon.setStyle(element.getStyle + "#opened")
    else
      icon.setStyle(element.getStyle + "#closed")
    */

    val text = element.findElementById("#tree-item-content")
    text.findNiftyControl("#label", classOf[Label]) match {
      case displayLabel: Label =>
        displayLabel.setText( stringify(item.getValue) )
      case _ =>
        val builder: LabelBuilder = new LabelBuilder(text.getId + "#label")
        builder.text( stringify(item.getValue) )
        builder.textHAlign(ElementBuilder.Align.Left)
        builder.width("*")
        builder.build(text.getNifty, text.getNifty.getCurrentScreen, text)
    }
    element.resetLayout()
    element.layoutElements()
  }

  def getWidth(element: Element, item: TreeItem[T]): Int = {
    var width = item.getIndent
    val icon  = element.findElementById("#tree-item-icon")
    val text  = element.findElementById("#tree-item-content")
    width += icon.getWidth
    text.findNiftyControl("#label", classOf[Label]) match {
      case displayLabel: Label =>
        val font: RenderFont = displayLabel.getElement.getRenderer(classOf[TextRenderer]).getFont
        width + font.getWidth( stringify(item.getValue) )
      case _ =>
        width
    }
  }
}
