package com.example.activitytracker

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController



//Not tested Yet
@RestController
@RequestMapping("/api/statistics")
class StatisticsController(
    val statisticsService: StatisticsService,
) {

    @GetMapping("/from/{userId}")
    fun getStatisticsFromUser(@PathVariable userId: Long) = statisticsService.getAllFromUser(userId)

    @PostMapping("/debug/collect/{userId}")
    fun collectStatistics(@PathVariable userId : Long) =
        statisticsService.collectStatsForUser(userId)

}