package be.angelcorp.omicronai.ai.actions

import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration.Duration
import java.util.concurrent.{TimeoutException, TimeUnit}
import akka.util.Timeout
import be.angelcorp.omicronai.gui.layerRender.{MultiplexRenderer, LayerRenderer}

case class SequencedAction( actions: Seq[Action] ) extends Action {
  implicit val timeout: Timeout = Duration(1, TimeUnit.MINUTES)
  lazy val preview: LayerRenderer = new MultiplexRenderer( actions.map( _.preview ) )

  case class FailedSequence(f: ActionExecutionException, remainingActions: List[Action])
    extends ActionExecutionException("Could not finish all sequence steps successfully", f.retryHint, f)

  override def execute(ai: ActionExecutor)(implicit context: ExecutionContext = ai.executionContext) = {
    val result = actions.foldLeft((None: Option[ActionExecutionException], Nil: List[Action]))( (result, action) => {
      result._1 match {
        case None =>
          (try {
            val res = Await.result(action.execute(ai), timeout.duration)
            res
          } catch {
            case e: TimeoutException => Some( TimedOut("Failed to execute action in sequence in the allowed time", e) )
          }, List(action))
        case e => (e, result._2 :+ action)
      }
    })
    Future.successful( result match {
      case (Some( err ), remaining) => Some( FailedSequence(err, remaining) )
      case _ => None
    })
  }

  override def recover(failure: ActionExecutionException) = failure match {
    case FailedSequence( f, remainingActions) =>
      remainingActions.head.recover( f ).map( replacement => {
        SequencedAction( replacement :: remainingActions.drop(1) )
      } )
    case _ => None
  }

}
