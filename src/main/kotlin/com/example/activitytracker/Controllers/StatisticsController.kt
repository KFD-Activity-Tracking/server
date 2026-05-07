package com.example.activitytracker

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class SessionMetricsDto(
    val avgCpu: Double = 0.0,
    val avgRam: Double = 0.0,
    val avgGpu: Double = 0.0,
)

@RestController
@RequestMapping("/api/statistics")
class StatisticsController(
    val statisticsService: StatisticsService,
    val jwtService: JwtService,
) {

    @GetMapping("/from/{userId}")
    fun getStatisticsFromUser(
        @PathVariable userId: Long,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") archived: Boolean,
    ) = statisticsService.getAllFromUser(userId, archived)

    @PostMapping("/debug/collect/{userId}")
    fun collectStatistics(@PathVariable userId: Long) = statisticsService.collectStatsForUser(userId)

    @PostMapping("/sessions/start")
    fun startSession(@RequestHeader("Authorization") authHeader: String): Map<String, Long> {
        val user = jwtService.extractUserFromHeader(authHeader)
        val session = statisticsService.startSession(user.id)
        return mapOf("sessionId" to session.id)
    }

    @PostMapping("/sessions/end")
    fun endSession(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody(required = false) metrics: SessionMetricsDto?,
    ) {
        val user = jwtService.extractUserFromHeader(authHeader)
        statisticsService.endSession(user.id, metrics = metrics)
    }

}