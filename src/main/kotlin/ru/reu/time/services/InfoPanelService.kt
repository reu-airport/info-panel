package ru.reu.time.services

import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.reu.time.client.TimeClient
import ru.reu.time.entities.Flight
import ru.reu.time.vo.TypeAirplane
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class InfoPanelService(
    private val rabbitTemplate: RabbitTemplate,
    private val timeClient: TimeClient
) {

    private val log = LoggerFactory.getLogger(javaClass)

    var flights = ConcurrentHashMap<UUID, Flight>()

    fun getById(flightId: UUID): Flight? = flights[flightId]

    fun save(flight: Flight): Flight {
        synchronized(this) {
            val uuid = UUID.randomUUID()
            return flights.getOrPut(uuid) {
                flight.apply { this.id = uuid }
            }
        }
    }

    fun flights(): List<Flight> {
        val time = timeClient.getTime()
        val instantTime = Instant.ofEpochMilli(time?.time!! + time.factor!!)
        return flights.filter { it.value.checkInBeginTime?.isBefore(instantTime) ?: false }
            .map { it.value.apply { this.id = it.key } }
    }

    @Scheduled(fixedDelay = 1000)
    fun checkFlights() {
        val time = timeClient.getTime()
        val instantTime = Instant.ofEpochMilli(time?.time!! + time.factor!!)
        flights
            .filter { it.value.checkInEndTime?.isAfter(instantTime) ?: false }
            .map { sendAirplaneEvent(it.value.apply { this.id = it.key }) }
    }

    @Scheduled(fixedDelay = 10000)
    fun createFlights() {
        if (flights.size > 5) return
        val time = timeClient.getTime()
        val instantTime = Instant.ofEpochMilli(time?.time!! + time.factor!!)
        (0..(5 - flights.size)).forEach { _ ->
            save(
                Flight(
                    null,
                    if ((0..100).random() > 51) TypeAirplane.ARRIVAL else TypeAirplane.DEPARTURE
                ).apply {
                    if (this.direction == TypeAirplane.ARRIVAL) {
                        this.checkInBeginTime = instantTime.plusSeconds(30)
                        this.checkInEndTime = instantTime.plusSeconds(40)
                        this.time = instantTime.plusSeconds(50)
                    } else {
                        this.checkInBeginTime = instantTime.minusSeconds(200)
                        this.checkInEndTime = instantTime.minusSeconds(100)
                        this.time = instantTime.plusSeconds(10)
                    }
                    this.airplane = null
                    this.hasBaggage = (0..100).random() > 51
                    this.hasVips = (0..100).random() < 51
                }
            ).also {
                log.info("Successful created flight: ${it.id}")
            }
        }
    }

    fun sendAirplaneEvent(flight: Flight) {
        rabbitTemplate.convertAndSend(
            "airplaneEvent",
            flight
        )
        log.info("Successful send to flight: ${flight.id}")
    }

}
