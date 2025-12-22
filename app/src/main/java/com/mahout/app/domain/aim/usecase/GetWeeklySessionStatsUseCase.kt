package com.mahout.app.domain.aim.usecase

import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.path.repository.SessionRepository
import java.time.ZoneId
import javax.inject.Inject

class GetWeeklySessionStatsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(): WeeklySessionStats {
        val zone = ZoneId.systemDefault()
        val now = timeProvider.nowInstant()
        val today = now.atZone(zone).toLocalDate()

        // Last 7 days INCLUDING today
        val fromDate = today.minusDays(6)
        val fromInstant = fromDate.atStartOfDay(zone).toInstant()

        val sessions = sessionRepository.getSessionsInRange(from = fromInstant, to = now)

        val totalMillis = sessions.sumOf { s ->
            if (s.endAt == null) {
                (now.toEpochMilli() - s.startAt.toEpochMilli()).coerceAtLeast(0L)
            } else {
                s.durationMillis.coerceAtLeast(0L)
            }
        }

        val activeDays = sessions
            .map { it.startAt.atZone(zone).toLocalDate() }
            .toSet()
            .size

        return WeeklySessionStats(
            totalMillis = totalMillis,
            sessionCount = sessions.size,
            activeDays = activeDays
        )
    }
}

data class WeeklySessionStats(
    val totalMillis: Long,
    val sessionCount: Int,
    val activeDays: Int
)
