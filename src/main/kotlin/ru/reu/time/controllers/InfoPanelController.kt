package ru.reu.time.controllers

import org.springframework.web.bind.annotation.*
import ru.reu.time.entities.Flight
import ru.reu.time.services.InfoPanelService
import java.util.*

@RestController
@RequestMapping("api/v1/info-panel")
class InfoPanelController(
    private val infoPanelService: InfoPanelService
) {

    @GetMapping("{flightId}")
    fun getFlight(@PathVariable flightId: UUID): Flight? = infoPanelService.getById(flightId)

    @GetMapping("all")
    fun getFlights(): List<Flight> = infoPanelService.flights()

    @PostMapping
    fun createFlight(@RequestBody flight: Flight): Flight = infoPanelService.save(flight)

}
