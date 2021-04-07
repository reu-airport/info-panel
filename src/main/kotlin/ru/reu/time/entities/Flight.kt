package ru.reu.time.entities

import ru.reu.time.vo.TypeAirplane
import java.time.Instant
import java.util.*

data class Flight(
    var id: UUID? = null,
    var direction: TypeAirplane? = null,
    var time: Instant? = null,
    var checkInBeginTime: Instant? = null,
    var checkInEndTime: Instant? = null,
    var hasVips: Boolean = false,
    var hasBaggage: Boolean = false,
    var airplane: UUID? = null,
)
