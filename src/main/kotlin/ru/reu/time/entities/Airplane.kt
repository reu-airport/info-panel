package ru.reu.time.entities

import java.util.*

data class Airplane(
    var id: UUID? = null,
    var capacity: Int? = null,
    var refuelNeeded: Boolean? = null,
    var isFlight: Boolean = false
)
