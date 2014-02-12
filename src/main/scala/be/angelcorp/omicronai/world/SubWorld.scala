package be.angelcorp.omicronai.world

import be.angelcorp.omicronai.Location

case class SubWorld(bounds: WorldBounds, states: Array[Array[(Location, WorldState)]])