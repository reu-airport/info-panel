package ru.reu.time.client

import retrofit2.Call
import retrofit2.http.GET
import ru.reu.time.vo.TimeVO

interface TimeInterface {

    @GET("api/v1/time")
    fun getTime(): Call<TimeVO>

}
