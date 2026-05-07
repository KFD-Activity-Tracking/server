package com.example.activitytracker

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@NoRepositoryBean
interface LongKeyRepository<T> : CrudRepository<T, Long>


@Repository
interface ActionRepository : LongKeyRepository<Action> {

    @Query("SELECT a FROM Action a WHERE TYPE(a) = MouseAction")
    fun getMouseActions(): List<MouseAction>

    @Query("SELECT a FROM Action a WHERE TYPE(a) = KeyboardAction")
    fun getKeyboardActions(): List<KeyboardAction>

    @Query("SELECT a FROM Action a WHERE TYPE(a) = AppAction")
    fun getAppActions(): List<AppAction>

    @Query("SELECT a FROM Action a WHERE a.user.id = :userId")
    fun findAllByUserId(@Param("userId") userId: Long): List<Action>

    fun findFirstByOrderByPerformedAtAsc(): Action?

}



@Repository
interface UserRepository : LongKeyRepository<Users> {

    fun findProjectionById(id: Long) : ProjectionUser?

    fun findProjectionByUsername(username: String): ProjectionUser?

    fun save(user: Users) : Users

}





@Repository
interface StatisticsRepository : LongKeyRepository<Statistics> {

    @Query("SELECT s FROM Statistics s WHERE s.user_id.id = :userId AND s.status = :status")
    fun findByUserIdAndStatus(
        @Param("userId") userId: Long,
        @Param("status") status: SessionStatus
    ): Statistics?

    @Query("SELECT s FROM Statistics s WHERE s.user_id.id = :userId")
    fun findAllByUserId(@Param("userId") userId: Long): List<Statistics>

    @Query("SELECT s FROM Statistics s WHERE s.user_id.id = :userId AND s.archived = :archived")
    fun findAllByUserIdAndArchived(
        @Param("userId") userId: Long,
        @Param("archived") archived: Boolean,
    ): List<Statistics>

    @Query("SELECT s FROM Statistics s WHERE s.status = :status")
    fun findAllByStatus(@Param("status") status: SessionStatus): List<Statistics>

    @Query("SELECT s FROM Statistics s WHERE s.status = :status AND s.archived = false AND s.end_time < :cutoff")
    fun findCompletedBeforeAndNotArchived(
        @Param("status") status: SessionStatus,
        @Param("cutoff") cutoff: java.time.LocalDateTime,
    ): List<Statistics>

}





@Repository
interface AppStatisticsRepository : LongKeyRepository<AppStatistics>










