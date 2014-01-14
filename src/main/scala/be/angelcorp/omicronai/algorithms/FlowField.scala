package be.angelcorp.omicronai.algorithms

import be.angelcorp.omicronai.{Direction, Location}

class FlowField {

}

case class DirectionVector( u: Double, v: Double, h: Double ) {

  def dot(mv: MovementVector) =
    u * mv.du + v * mv.dv + h * mv.dh

}

class DirectionMap(destination: Location) {

  val flowField = {
    val floodcost = FloodFill.fill( destination )
    Field.tabulate(destination.size)( location => {
      val bestNeighbour = location.neighbours.minBy( entry => floodcost(entry._2) )

      val dir = bestNeighbour._1

      val s = math.sqrt(dir.du * dir.du + dir.dv * dir.dv + dir.dh * dir.dh)
      new DirectionVector(dir.du/s, dir.dv/s, dir.dh/s)
    } )
  }

  val w_max = 1.0

  def costOfMoving( mv: MovementVector ) = {
    val w_ab = mv.cost
    w_ab + 0.25 * w_max * ( 2 - flowField(mv.from).dot(mv) - flowField(mv.to).dot(mv) )
  }

  def update(move: MovementVector) = {
    val updateLocations = (move.from.neighbours.values ++ move.to.neighbours.values ++ Seq(move.from, move.to)).toStream.distinct
    val mv = (move.du.toDouble, move.dv.toDouble, move.dh.toDouble)
    updateLocations.foreach( l => updateCell(l, mv))
  }

  def updateCell(l: Location, move: (Double, Double, Double)) = {
    val vector = flowField(l)
    val α = 0.5
    val u = (1 - α) * vector.u + α * move._1
    val v = (1 - α) * vector.v + α * move._2
    val h = (1 - α) * vector.h + α * move._3
    val s = math.sqrt( u*u + v*v + h*h )
    flowField(l) = new DirectionVector(u/s,v/s,h/s)
  }

}

class MovementVector(val from: Location, val to: Location, val cost: Double) {

  def du = from δu to
  def dv = from δv to
  def dh = from δh to

}