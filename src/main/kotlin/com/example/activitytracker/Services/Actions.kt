package com.example.activitytracker

import org.springframework.stereotype.Service
import kotlin.collections.addAll
import kotlin.collections.iterator


interface ActionService {
    fun addMouseAction(act: MouseAction) : Action
    fun addKeyboardAction(act: KeyboardAction) : Action
    fun addAppAction(act: AppAction) : Action

    fun saveAllActions(actions: List<Action>) : List<Action>

    fun getAllActionsFromUser(userId: Long): List<Action>

    fun removeAllById(actions : List<Long>)
}



@Service
class ActionServiceImpl(
    var actionDao: ActionRepository,
) :
    ActionService {

    override fun addMouseAction(act: MouseAction) = actionDao.save(act)

    override fun addKeyboardAction(act: KeyboardAction) = actionDao.save(act)

    override fun addAppAction(act: AppAction) = actionDao.save(act)

    override fun saveAllActions(actions: List<Action>): List<Action> {

        println("DEBUG $actions")
        val result = mutableListOf<Action>()

        val groups = actions.groupBy { it::class.java }

        for (group in groups) {

            println("DEBUG ${group.key}")
            when (group.key) {
                MouseAction::class.java -> {
                    println("DEBUG ${group.value} actions")
                    result.addAll(actionDao.saveAll<MouseAction>(
                        group.value as List<MouseAction>))
                }
                KeyboardAction::class.java -> {
                    result.addAll(actionDao.saveAll<KeyboardAction>(
                        group.value as List<KeyboardAction>))
                }
                AppAction::class.java -> {
                    result.addAll(actionDao.saveAll<AppAction>(
                        group.value as List<AppAction>))
                }
            }
        }



        return result
    }

    override fun getAllActionsFromUser(userId: Long): List<Action> = actionDao.findAll().filter { it.user_id == userId }.toList()
    override fun removeAllById(actions: List<Long>) = actionDao.deleteAllById(actions)

}









