package com.example.activitytracker

import org.springframework.stereotype.Service
import java.time.LocalDateTime


interface StatisticsService{

    fun collectStatsForUser(userId: Long): Statistics

    fun getAllFromUser(userId: Long): List<Statistics>

}



@Service
class StatisticsServiceImpl (
    val userService: UserServiceImpl,
    val actionService: ActionService,

    val statisticsDao: StatisticsRepository
) : StatisticsService {

    override fun collectStatsForUser(userId: Long): Statistics {

        val actions = actionService.getAllActionsFromUser(userId)
        val result = Statistics()

        result.user_id = userService.getUserById(userId)
        result.start_time = LocalDateTime.now()
        result.end_time = LocalDateTime.now()

        actionService.removeAllById(actions.map { it.id })

        statisticsDao.save(result)

        return result
    }

    override fun getAllFromUser(userId: Long): List<Statistics> = statisticsDao.findAll().filter { it.user_id?.id == userId }.toList()

}




