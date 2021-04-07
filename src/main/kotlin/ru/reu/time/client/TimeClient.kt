package ru.reu.time.client

import org.springframework.stereotype.Component
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

@Component
class TimeClient {

    var retrofit = Retrofit.Builder()
        .baseUrl("http://localhost:8081/")
        .addConverterFactory(JacksonConverterFactory.create())
        .build().create(TimeInterface::class.java)

    fun getTime() = retrofit.getTime().execute().body()

}
