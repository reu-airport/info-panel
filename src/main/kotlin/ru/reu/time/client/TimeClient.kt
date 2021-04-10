package ru.reu.time.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.reu.time.vo.TimeVO
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
class TimeClient {

    private val log = LoggerFactory.getLogger(javaClass)

    private val mapper = jacksonObjectMapper()

    fun getTimeCurrent(): TimeVO {
        println("Create request")
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://206.189.60.128:8083/api/v1/time"))
            .build()
        println("Finished create request")
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        println(response.body())
        return mapper.readValue(response.body(), TimeVO::class.java)
    }

    fun getTime() = getTimeCurrent()

}
