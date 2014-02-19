package be.angelcorp.omicron.base

import com.google.common.base.Optional
import com.lyndir.omicron.api.util.Maybe.Presence._

object Conversions {

  implicit def toOption[T]( o: Optional[T] ) = if (o.isPresent) Some(o.get) else None

  implicit def toMaybe[T]( o: com.lyndir.omicron.api.util.Maybe[T] ) = o.presence() match {
    case ABSENT  => Absent
    case UNKNOWN => Unknown
    case PRESENT => Present(o.get())
  }

}


sealed abstract class Maybe[+A] {
  self =>

  /** Returns true if the option is $none, false otherwise. */
  def isEmpty: Boolean

  /** Returns true if the option is unknown, false otherwise. */
  def isUnknown: Boolean

  /** Returns true if the option is an instance of $some, false otherwise. */
  def isDefined: Boolean = !isEmpty && !isUnknown

  /** Returns the maybe's value. */
  def get: A

  /** Returns an option view of this Maybe */
  def asOption = if (isDefined) Some(this.get) else None

}

/** Class `Present[A]` represents existing values of type */
final case class Present[+A](x: A) extends Maybe[A] {
  def isUnknown = false
  def isEmpty   = false
  def get = x
}

/** This case object represents unknown values. */
case object Unknown extends Maybe[Nothing] {
  def isUnknown = true
  def isEmpty   = false
  def get       = throw new NoSuchElementException("Absent.get")
}

/** This case object represents non-existent values. */
case object Absent extends Maybe[Nothing] {
  def isUnknown = false
  def isEmpty   = true
  def get = throw new NoSuchElementException("None.get")
}
