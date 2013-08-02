package be.angelcorp.omicronai

import scala.collection.mutable
import java.util.concurrent.atomic.AtomicInteger

class Namer[T]( nameFunction: T => String ) {

  val nameCount = mutable.Map[String, AtomicInteger]()

  def nameFor( item: T ): String =  {
    val baseName = nameFunction(item)

    val count = nameCount.getOrElseUpdate( baseName, new AtomicInteger(0) ).incrementAndGet()
    s"$baseName $count".replaceAll("""\s+""", "_")
  }

}
