package be.angelcorp.omicronai.ai.actions

import be.angelcorp.omicronai.gui.layerRender.LayerRenderer
import be.angelcorp.omicronai.ai.{Never, ActionExecutionException, ActionExecutor}
import scala.util.{Failure, Success, Try}
import scala.concurrent.{ExecutionContext, Promise, Future}

trait Action {

  def execute(ai: ActionExecutor)(implicit context: ExecutionContext = ai.executionContext): Future[Option[ActionExecutionException]]

  def preview: LayerRenderer

  def recover( failure: ActionExecutionException ): Option[Action] = None

  def wasSuccess[T]( t: Future[Try[T]] )(implicit context: ExecutionContext) = t.map {
    case Success(_) => None
    case Failure(f: ActionExecutionException) => Some(f)
    case Failure(f) => Some(new ActionExecutionException(s"Unknown exception in the execution of ${getClass.getSimpleName}", Never))
  }

}
