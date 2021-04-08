package ru.reu.time.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.reu.time.client.TimeClient
import ru.reu.time.entities.Airplane
import ru.reu.time.entities.Flight
import ru.reu.time.vo.FlightVO
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

    private val mapper = jacksonObjectMapper()

    var flights = ConcurrentHashMap<UUID, Flight>()
    var airplanes = ConcurrentHashMap<UUID, Airplane>()

    fun getById(flightId: UUID): Flight? = flights[flightId]

    fun save(flight: Flight): Flight {
        synchronized(flights) {
            val uuid = UUID.randomUUID()
            return flights.getOrPut(uuid) {
                flight.apply { this.id = uuid }
            }
        }
    }

    fun saveAirplane(airplane: Airplane): Airplane {
        synchronized(airplanes) {
            val uuid = UUID.randomUUID()
            return airplanes.getOrPut(uuid) {
                airplane.apply { this.id = uuid }
            }
        }
    }

    fun flights(): List<Flight> {
        val time = timeClient.getTime()
        val instantTime = Instant.ofEpochMilli(time?.time!!)
        return flights.filter { it.value.checkInBeginTime?.isBefore(instantTime) ?: false }
            .map { it.value.apply { this.id = it.key } }
    }

    @Scheduled(fixedDelay = 1000)
    fun checkFlights() {
        val time = timeClient.getTime()
        val instantTime = Instant.ofEpochMilli(time?.time!!)
        flights
            .filter { instantTime?.isAfter(it.value.time) ?: false }
            .map {
                sendAirplaneEvent(it.value.apply { this.id = it.key })
                airplanes.remove(it.value.airplane.id)
                flights.remove(it.key)
            }
    }

    @Scheduled(fixedDelay = 10000)
    fun createFlights() {
        if (flights.size > 5) return
        val time = timeClient.getTime()
        val instantTime = Instant.ofEpochMilli(time?.time!!)
        (0..(5 - flights.size)).forEach { _ ->
            val airplaneId = saveAirplane(
                Airplane(
                    null,
                    (0..100).random(),
                    (0..100).random() > 51
                )
            ).also {
                log.info("Successful created airplane: ${it.id}")
            }
            save(
                Flight(
                    null,
                    if ((0..100).random() > 51) TypeAirplane.ARRIVAL else TypeAirplane.DEPARTURE,
                    airplane = airplaneId
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
                    this.hasBaggage = (0..100).random() > 51
                    this.hasVips = (0..100).random() < 51
                    this.gateNum = (0..100).random()
                }
            ).also {
                log.info("Successful created flight: ${it.id}")
                airplanes.getValue(airplaneId.id!!).isFlight = true
                log.info("Successful added flight: ${it.id} to $airplaneId")
            }
        }
    }

    fun sendAirplaneEvent(flight: Flight) {
        rabbitTemplate.convertAndSend(
            "airplaneEvent",
            mapper.writeValueAsString(
                FlightVO(
                    flight.id,
                    flight.direction,
                    flight.time!!.toEpochMilli(),
                    flight.hasVips,
                    flight.hasBaggage,
                    flight.airplane,
                    flight.gateNum
                )
            )
        )
        log.info("Successful send to flight: $flight")
    }

}
