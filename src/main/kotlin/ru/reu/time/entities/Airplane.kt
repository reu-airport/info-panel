package ru.reu.time.entities

import ru.reu.time.vo.TypeAirplane
import java.util.*

data class Airplane(
    var id: UUID? = null,
    var capacity: Int? = null,
    var refuelNeeded: Boolean? = null,
    var isFlight: Boolean = false,
    var direction: TypeAirplane? = null
)
