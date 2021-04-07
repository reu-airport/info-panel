package ru.reu.time.vo

import java.time.Instant
import java.util.*

data class AirplaneEvent(
    var planeId: UUID? = null,
    var flightId: UUID? = null,
    var type: TypeAirplane? = null,
    var timestamp: Instant? = null
)
