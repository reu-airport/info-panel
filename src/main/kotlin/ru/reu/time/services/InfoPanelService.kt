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

    var ttl = 10
    lateinit var currentTime: Instant

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
        val instantTime = time()
        return flights
            .filter { it.value.direction == TypeAirplane.ARRIVAL }
            .filter { it.value.checkInBeginTime?.isBefore(instantTime) ?: false }
            .map { it.value.apply { this.id = it.key } }
    }

    @Scheduled(fixedDelay = 1000)
    fun checkFlights() {
        log.info("Started checkFlights")
        val instantTime = time()
        log.info("Getting time $instantTime")
        flights
            .filter { instantTime.isAfter(it.value.time) }
            .map {
                sendAirplaneEvent(it.value.apply { this.id = it.key })
                airplanes.get(it.value.airplane?.id)?.isFlight = false
                airplanes.get(it.value.airplane?.id)?.direction =
                    if (airplanes.get(it.value.airplane?.id)?.direction == TypeAirplane.ARRIVAL)
                        TypeAirplane.DEPARTURE
                    else TypeAirplane.ARRIVAL
                flights.remove(it.key)
            }
    }

    @Scheduled(fixedDelay = 10000)
    fun createFlights() {
        if (flights.size > 2) return
        log.info("Started getTime")
        val instantTime = time()
        log.info("Getting time $instantTime")


        (0..(1 - airplanes.size)).forEach { i ->
            if (airplanes.size > 2) return@forEach
            saveAirplane(
                Airplane(
                    null,
                    (0..100).random(),
                    (0..100).random() > 51,
                    isFlight = false,
                    if (i == 1) TypeAirplane.DEPARTURE else TypeAirplane.ARRIVAL
                )
            ).also {
                log.info("Successful created airplane: ${it.id}")
            }
        }
        
        (0..(2 - flights.size)).forEach { _ ->
            val direction = if ((0..100).random() > 51) TypeAirplane.ARRIVAL else TypeAirplane.DEPARTURE
            if (airplanes
                    .filter { air -> air.value.direction == direction }
                    .filter { air -> !air.value.isFlight }.values.isEmpty()
            ) return

            save(
                Flight(
                    null,
                    direction
                ).apply {
                    if (this.direction == TypeAirplane.ARRIVAL) {
                        this.checkInBeginTime = instantTime.minusSeconds(0L + (1..10).random())
                        this.checkInEndTime = instantTime.plusSeconds(30L + (5..20).random())
                        this.time = instantTime.plusSeconds(40L + (5..10).random())
                    } else {
                        this.checkInBeginTime = instantTime.minusSeconds(200)
                        this.checkInEndTime = instantTime.minusSeconds(100)
                        this.time = instantTime.plusSeconds(10)
                    }
                    this.hasBaggage = (0..100).random() > 51
                    this.hasVips = (0..100).random() < 51
                    this.gateNum = (1..4).random()
                }
            ).also {
                log.info("Successful created flight: ${it.id}")
                val airplane = airplanes
                    .filter { air -> air.value.direction == it.direction }
                    .filter { air -> !air.value.isFlight }.values.first()
                airplanes.getValue(airplane.id!!).isFlight = true
                airplanes.getValue(airplane.id!!).direction = it.direction!!
                airplanes.getValue(airplane.id!!).refuelNeeded = (0..100).random() > 51
                flights.get(it.id)?.airplane = airplane
                log.info("Successful added flight: $it to ${airplane.id}")
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
                    flight.airplane!!,
                    flight.gateNum
                )
            )
        )
        log.info("Successful send to flight: $flight")
    }

    private fun time(): Instant {
        return if (ttl == 0) {
            val time = timeClient.getTime()
            ttl = 20
            currentTime = Instant.ofEpochMilli(time.time)
            currentTime
        } else {
            ttl--
            currentTime
        }
    }

}
