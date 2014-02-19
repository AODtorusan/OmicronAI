package be.angelcorp.omicron.base.algorithms

import scala.reflect.ClassTag
import be.angelcorp.omicron.base.Location
import be.angelcorp.omicron.base.world.WorldBounds

trait Field[T] {

  def apply( l: Location ): T
  def update(l: Location, value: T): Unit

  def map[T2: ClassTag]( transform: T => T2 ): Field[T2]

}

object  Field {

  def fill[T: ClassTag]( sz: WorldBounds )(elem: => T) =
    new ArrayField[T]( Array.fill(sz.uSize, sz.vSize, sz.hSize)(elem), sz )

  def ofDim[T: ClassTag]( sz: WorldBounds ) =
    new ArrayField[T]( Array.ofDim(sz.uSize, sz.vSize, sz.hSize), sz )

  def tabulate[T: ClassTag]( sz: WorldBounds )(f: Location => T) =
    new ArrayField[T]( Array.tabulate[T](sz.uSize, sz.vSize, sz.hSize)((u, v, h) => f(new Location(u, v, h, sz))), sz )

}

class ArrayField[T]( val data: Array[Array[Array[T]]], val size: WorldBounds ) extends Field[T] {

  def apply( l: Location ) = data(l.u)(l.v)(l.h)
  def update(l: Location, value: T) = data(l.u)(l.v)(l.h) = value

  def map[T2: ClassTag]( transform: T => T2 ) = new ArrayField( data.map(_.map(_.map(transform).toArray)), size )

}