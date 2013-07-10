package be.angelcorp.omicronai

import com.google.common.base.Optional

object Conversions {

  implicit def toOption[T]( o: Optional[T] ) = if (o.isPresent) Some(o.get) else None

}
