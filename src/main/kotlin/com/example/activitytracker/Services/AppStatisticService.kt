package com.example.activitytracker.Services

import com.example.activitytracker.AppStatistics
import com.example.activitytracker.AppStatisticsRepository
import org.springframework.stereotype.Service


interface AppStatisticService {

    fun saveAll(apps: List<AppStatistics>)

}




@Service
class AppStatisticsServiceImpl(
    val appStatisticsRepository: AppStatisticsRepository,
) : AppStatisticService {
    override fun saveAll(apps: List<AppStatistics>) {
        appStatisticsRepository.saveAll(apps)
    }

}































