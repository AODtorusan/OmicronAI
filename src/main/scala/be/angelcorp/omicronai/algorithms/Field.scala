package be.angelcorp.omicronai.algorithms

import be.angelcorp.omicronai.Location
import scala.reflect.ClassTag

class Field[T]( val data: Array[Array[Array[T]]], val size: WorldSize ) {

  def apply( l: Location ) = data(l.u)(l.v)(l.h)
  def update(l: Location, value: T) = data(l.u)(l.v)(l.h) = value

  def map[T2: ClassTag]( transform: T => T2 ) = new Field( data.map(_.map(_.map(transform).toArray)), size )

}
object  Field {

  def fill[T: ClassTag]( sz: WorldSize )(elem: => T) =
    new Field[T]( Array.fill(sz.uSize, sz.vSize, sz.hSize)(elem), sz )

  def ofDim[T: ClassTag]( sz: WorldSize ) =
    new Field[T]( Array.ofDim(sz.uSize, sz.vSize, sz.hSize), sz )

  def tabulate[T: ClassTag]( sz: WorldSize )(f: Location => T) =
    new Field[T]( Array.tabulate[T](sz.uSize, sz.vSize, sz.hSize)((u, v, h) => f(new Location(u, v, h, sz))), sz )

}