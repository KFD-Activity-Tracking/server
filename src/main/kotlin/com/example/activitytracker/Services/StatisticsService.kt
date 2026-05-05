package com.example.activitytracker

import com.example.activitytracker.Services.ActionAnalysisService
import com.example.activitytracker.Services.AppStatisticService
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
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

    val actionAnalysisService: ActionAnalysisService,
) : StatisticsService {



    override fun collectStatsForUser(userId: Long): Statistics {

        val performAiEval = true



        val actions = actionService.getAllActionsFromUser(userId)


        val statistic = Statistics()
        statistic.user_id = Users(id = userId)

        val sorted = actions.sortedBy { it.performedAt }

        statistic.start_time = sorted.firstOrNull()?.performedAt ?: LocalDateTime.now(ZoneOffset.UTC)
        statistic.end_time = sorted.lastOrNull()?.performedAt ?: LocalDateTime.now(ZoneOffset.UTC)
        val all_time_seconds : Long = statistic.start_time.toEpochSecond(ZoneOffset.UTC)-statistic.end_time.toEpochSecond(ZoneOffset.UTC)

        val app_actions = actions.filter { it is AppAction }.map { it as AppAction }.groupBy { it.app_name }
        val mouse_actions = actions.filter { it is MouseAction }.map { it as MouseAction }
        val keyboard_actions = actions.filter { it is KeyboardAction }.map { it as KeyboardAction }


        app_actions.forEach { app_name, named ->
            val app_statistic = AppStatistics()
            app_statistic.statistics_id = statistic
            app_statistic.app_name = app_name
            app_statistic.number_of_exits = named.size

            app_statistic.time_spent = -1L.NOT_COMPLETED
            statistic.app_statistics.add(app_statistic)
        }


        var mouse_moves : Double = 0.0
        for (i in 1..<mouse_actions.size) {
            var dx = mouse_actions[i].delta_x-mouse_actions[i-1].delta_x
            var dy = mouse_actions[i].delta_y-mouse_actions[i-1].delta_y
            mouse_moves += sqrt((dx*dx+dy*dy).toDouble())
        }
        mouse_moves*=100

        val heatMapWidth = 100
        val heatMapHeight = 100
        var heat_map = MutableList<Int>(heatMapHeight*heatMapWidth, {0})
        mouse_actions.forEach {
            val mapx = (it.delta_x*heatMapWidth).toInt().coerceIn(0, heatMapWidth-1)
            val mapy = (it.delta_y*heatMapHeight).toInt().coerceIn(0, heatMapHeight-1)
            val realIx = mapy*heatMapWidth+mapx
            heat_map[realIx]++
        }
        statistic.heat_map_width = heatMapWidth
        statistic.heat_map = String(List<Char>(heat_map.size, {min(255, heat_map[it]).toChar()}).toCharArray())



        val time_stamps : Int  = max(20, min(all_time_seconds.toInt()/60/5, 500))
        val clicks_per_stamp = MutableList<Int>(time_stamps, {0})
        mouse_actions.filter { it.is_click }.forEach {
            val passed = it.performedAt.toEpochSecond(ZoneOffset.UTC) -
                            statistic.start_time.toEpochSecond(ZoneOffset.UTC)
            val relative = time_stamps*(1.0*passed/all_time_seconds).toInt().coerceIn(0,time_stamps-1)
            clicks_per_stamp[relative.toInt()]+=1
        }

        statistic.clicks_over_time = String(CharArray(time_stamps, {min(255,clicks_per_stamp[it]).toChar()}))




        statistic.mouse_movement = mouse_moves.toLong()

        statistic.keyboard_clicks = keyboard_actions.size
        statistic.mouse_clicks = mouse_actions.count { it.is_click }
        statistic.keyboard_to_mouse_coef = keyboard_actions.size.toFloat()/max(1,statistic.mouse_clicks)


        statistic.active_time = 0L.NOT_COMPLETED

        statistic.idle_time = all_time_seconds - statistic.active_time



        if (performAiEval){
            try {
                println("Starting AI evaluation for user $userId...")

                // blocking since we are in a non-suspend function
                val analysis = runBlocking {
                    actionAnalysisService.analyzeActions(actions, userId)
                }


                statistic.ai_eval = "Analysis performed at ${LocalDateTime.now().toString()} \n" +
                        "Using ${actionAnalysisService.getModelName()} as model for user ${userId} \n" +
                        "Analyzed: ${actions.size} actions \n" +
                        "Anomalies: ${analysis.is_anomalous}, fraud probability: ${analysis.fraud_probability} \n" +
                        "Issues: ${analysis.issues.joinToString { it.substring(0,min(80, it.length)) }} \n"

                println("DEBUG ${statistic.ai_eval}")

            } catch (e: Exception) {
                println("DEBUG AI evaluation failed for user $userId: ${e.message}")
                val errorEval = mapOf(
                    "error" to e.message,
                    "timestamp" to System.currentTimeMillis(),
                    "fallback" to "rule-based"
                )
                statistic.ai_eval = errorEval.toString()
            }
        }





        println("Other later").NOT_COMPLETED    //other fields





        appStatisticService.saveAll(statistic.app_statistics)

        actionService.removeAllById(actions.map { it.id })
        statisticsDao.save(statistic)

        return statistic
    }

    override fun getAllFromUser(userId: Long): List<Statistics> = statisticsDao.findAll().filter { it.user_id?.id == userId }.toList()
    override fun collectAllStatistics() {
        println("debug Collecting statistics!! ").NOT_COMPLETED  //debug

        val user_action = actionService.getAllActions().groupBy { it.user.id }

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




















