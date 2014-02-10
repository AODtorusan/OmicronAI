package be.angelcorp.omicronai.ai.actions

import be.angelcorp.omicronai.gui.layerRender.LayerRenderer
import be.angelcorp.omicronai.ai.{ActionExecutionException, ActionExecutor}
import scala.util.{Failure, Success, Try}

trait Action {

  def execute(ai: ActionExecutor): Option[ActionExecutionException]

  def preview: LayerRenderer

  def recover( failure: ActionExecutionException ): Option[Action] = None

  def wasSuccess[T]( t: Try[T]  ) = t match {
    case Success( _ ) => None
    case Failure( f: ActionExecutionException ) => Some(f)
    case Failure( f ) => Some( new ActionExecutionException(s"Unknown exception in the execution of ${getClass.getSimpleName}", false) )
  }

}
