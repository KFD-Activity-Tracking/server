package com.example.activitytracker

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository



interface LongKeyRepository<T> : CrudRepository<T, Long>


@Repository
interface ActionRepository : LongKeyRepository<Action> {

    @Query("SELECT a FROM Action a WHERE TYPE(a) = MouseAction")
    fun GetMouseActions(): List<MouseAction>

    @Query("SELECT a FROM Action a WHERE TYPE(a) = KeyboardAction")
    fun GetKeyboardActions(): List<KeyboardAction>

    @Query("SELECT a FROM Action a WHERE TYPE(a) = AppAction")
    fun GetAppActions(): List<AppAction>

}



@Repository
interface UserRepository : LongKeyRepository<User>





@Repository
interface StatisticsRepository : LongKeyRepository<Statistics>





@Repository
interface AppStatisticsRepository : LongKeyRepository<AppStatistics>










