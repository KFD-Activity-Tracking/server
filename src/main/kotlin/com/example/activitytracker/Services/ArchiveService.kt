package com.example.activitytracker.Services

import com.example.activitytracker.SessionStatus
import com.example.activitytracker.StatisticsRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class ArchiveService(
    private val statisticsDao: StatisticsRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${archive.days:30}")
    private var archiveDays: Long = 30

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    fun archiveOldStatistics() {
        val cutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(archiveDays)
        val toArchive = statisticsDao.findCompletedBeforeAndNotArchived(SessionStatus.COMPLETED, cutoff)
        if (toArchive.isEmpty()) return

        val now = LocalDateTime.now(ZoneOffset.UTC)
        toArchive.forEach {
            it.archived = true
            it.archived_at = now
        }
        statisticsDao.saveAll(toArchive)
        logger.info("Archived ${toArchive.size} statistics records older than $archiveDays days")
    }
}
