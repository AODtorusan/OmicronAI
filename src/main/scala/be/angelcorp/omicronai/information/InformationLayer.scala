package be.angelcorp.omicronai.information

/**
 * Container to hold additional information on a goal/action
 */
trait InformationLayer {

  def name: String

  override def toString = name

}
