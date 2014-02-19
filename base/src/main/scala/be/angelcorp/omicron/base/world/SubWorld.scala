package be.angelcorp.omicron.base.world

import be.angelcorp.omicron.base.Location

case class SubWorld(bounds: WorldBounds, states: Array[Array[(Location, WorldState)]])