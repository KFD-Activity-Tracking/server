package com.example.activitytracker

import com.example.activitytracker.Services.AppStatisticService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.minutes


interface StatisticsService{

    fun collectStatsForUser(userId: Long): Statistics

    fun getAllFromUser(userId: Long): List<Statistics>

    fun collectAllStatistics()

}



@Service
class StatisticsServiceImpl (
    val userService: UserServiceImpl,
    val actionService: ActionService,

    val statisticsDao: StatisticsRepository,
    val appStatisticService: AppStatisticService,
) : StatisticsService {

    override fun collectStatsForUser(userId: Long): Statistics {

        val actions = actionService.getAllActionsFromUser(userId)


        val statistic = Statistics()
        statistic.user_id = Users(userId)

        val sorted = actions.sortedBy { it.performedAt }

        statistic.start_time = sorted.first().performedAt
        statistic.end_time = sorted.last().performedAt


        val app_actions = actions.filter { it is AppAction }.map { it as AppAction }.groupBy { it.app_name }

        app_actions.forEach { app_name, named ->
            val app_statistic = AppStatistics()
            app_statistic.statistics_id = statistic
            app_statistic.app_name = app_name
            app_statistic.number_of_exits = named.size

            app_statistic.time_spent = -1L.NOT_COMPLETED
            statistic.app_statistics.add(app_statistic)
        }


        statistic.active_time = 0L.NOT_COMPLETED

        statistic.idle_time = statistic.logout_time - statistic.logout_time - statistic.active_time

        println("Other later").NOT_COMPLETED    //other fields






        appStatisticService.saveAll(statistic.app_statistics)

        actionService.removeAllById(actions.map { it.id })
        statisticsDao.save(statistic)

        return statistic
    }

    override fun getAllFromUser(userId: Long): List<Statistics> = statisticsDao.findAll().filter { it.user_id?.id == userId }.toList()
    override fun collectAllStatistics() {
        println("debug Collecting statistics!! ").NOT_COMPLETED  //debug

        val user_action = actionService.getAllActions().groupBy { it.userId }

        user_action.forEach { user_id, actions ->
            collectStatsForUser(user_id)
        }
    }

}





@Component
class StatisticCollector(
    val statisticsService: StatisticsService,
    val actionService: ActionService,
) {

    val collectPeriod : Long = 2.minutes.inWholeMilliseconds.NOT_COMPLETED    //change to 60 minutes


    @Scheduled(fixedDelay = 60_000)
    fun scheduleCollection(){

        val earliestAction = actionService.getEarliestAction()
        if (earliestAction==null){
            return
        }

        if (earliestAction.performedAt.toInstant(ZoneOffset.UTC).toEpochMilli() > (System.currentTimeMillis()-collectPeriod)){
            return
        }

        statisticsService.collectAllStatistics()

    }

}




















