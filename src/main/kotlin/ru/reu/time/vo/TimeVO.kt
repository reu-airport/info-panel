package ru.reu.time.vo

import com.fasterxml.jackson.annotation.JsonProperty

data class TimeVO(
    @JsonProperty("time")
    val time: Long,
    @JsonProperty("factor")
    val factor: Long?
)
