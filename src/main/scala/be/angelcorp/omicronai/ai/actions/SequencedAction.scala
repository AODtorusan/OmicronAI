package be.angelcorp.omicronai.ai.actions

import be.angelcorp.omicronai.ai.{ActionExecutionException, ActionExecutor}
import be.angelcorp.omicronai.gui.layerRender.{MultiplexRenderer, LayerRenderer}

case class SequencedAction( actions: Seq[Action] ) extends Action {
  lazy val preview: LayerRenderer = new MultiplexRenderer( actions.map( _.preview ) )

  case class FailedSequence(f: ActionExecutionException, remainingActions: List[Action])
    extends ActionExecutionException("Could not finish all sequence steps successfully", f.isTurnConstrained, f)

  override def execute(ai: ActionExecutor): Option[ActionExecutionException] = {
    val result = actions.foldLeft((None: Option[ActionExecutionException], Nil: List[Action]))( (result, action) => {
      result._1 match {
        case None => (action.execute(ai), List(action))
        case e    => (e, result._2 :+ action)
      }
    })
    result match {
      case (Some( err ), remaining) => Some( FailedSequence(err, remaining) )
      case _ => None
    }
  }

  override def recover(failure: ActionExecutionException) = failure match {
    case FailedSequence( f, remainingActions) =>
      remainingActions.head.recover( f ).map( replacement => {
        SequencedAction( replacement :: remainingActions.drop(1) )
      } )
    case _ => None
  }

}
