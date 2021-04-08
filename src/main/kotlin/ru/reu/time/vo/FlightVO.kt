package ru.reu.time.vo

import ru.reu.time.entities.Airplane
import ru.reu.time.vo.TypeAirplane
import java.time.Instant
import java.util.*

data class FlightVO(
    var id: UUID? = null,
    var direction: TypeAirplane? = null,
    var time: Long? = null,
    var hasVips: Boolean = false,
    var hasBaggage: Boolean = false,
    var airplane: Airplane,
    var gateNum: Int? = null
)
