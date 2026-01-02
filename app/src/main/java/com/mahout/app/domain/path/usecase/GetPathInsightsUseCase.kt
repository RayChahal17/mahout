package com.mahout.app.domain.path.usecase

import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.path.model.Session
import com.mahout.app.domain.path.model.SessionSource
import com.mahout.app.domain.path.repository.ActionRepository
import com.mahout.app.domain.path.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Computes insights data for the Path Insights screen.
 * Supports different time periods (Today, Week, Month, Year) and filters (Logged vs Deep Focus).
 */
class GetPathInsightsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val actionRepository: ActionRepository,
    private val timeProvider: TimeProvider
) {

    enum class TimePeriod {
        TODAY,
        WEEK,
        MONTH,
        YEAR
    }

    enum class FilterType {
        LOGGED,      // All sessions (TIMER + MANUAL)
        DEEP_FOCUS   // Only TIMER sessions
    }

    data class TaskInsight(
        val actionId: String,
        val actionTitle: String,
        val totalMillis: Long,
        val sessionCount: Int
    )

    data class Result(
        val totalMillis: Long,
        val topTask: TaskInsight?,
        val sessionCount: Int,
        val tasks: List<TaskInsight>
    )

    suspend operator fun invoke(
        selectedDate: LocalDate,
        period: TimePeriod,
        filterType: FilterType
    ): Result = withContext(Dispatchers.Default) {
        val zone = ZoneId.systemDefault()
        val now = timeProvider.nowInstant()

        // Calculate time range based on period
        val (from, to) = when (period) {
            TimePeriod.TODAY -> {
                val dayStart = selectedDate.atStartOfDay(zone).toInstant()
                val dayEnd = selectedDate.plusDays(1).atStartOfDay(zone).toInstant()
                dayStart to minOf(dayEnd, now)
            }
            TimePeriod.WEEK -> {
                val weekStart = selectedDate
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay(zone)
                    .toInstant()
                val weekEnd = weekStart.plus(7, ChronoUnit.DAYS)
                weekStart to minOf(weekEnd, now)
            }
            TimePeriod.MONTH -> {
                val monthStart = selectedDate.withDayOfMonth(1).atStartOfDay(zone).toInstant()
                val monthEnd = selectedDate.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant()
                monthStart to minOf(monthEnd, now)
            }
            TimePeriod.YEAR -> {
                val yearStart = selectedDate.withDayOfYear(1).atStartOfDay(zone).toInstant()
                val yearEnd = selectedDate.withDayOfYear(1).plusYears(1).atStartOfDay(zone).toInstant()
                yearStart to minOf(yearEnd, now)
            }
        }

        // Fetch sessions in range
        val allSessions = sessionRepository.getSessionsInRange(from, to)

        // Filter by type
        val filteredSessions = when (filterType) {
            FilterType.LOGGED -> allSessions
            FilterType.DEEP_FOCUS -> allSessions.filter { it.source == SessionSource.TIMER }
        }

        // Calculate total time (handle in-progress sessions)
        val totalMillis = filteredSessions.sumOf { session ->
            if (session.endAt == null) {
                (now.toEpochMilli() - session.startAt.toEpochMilli()).coerceAtLeast(0L)
            } else {
                session.durationMillis.coerceAtLeast(0L)
            }
        }

        // Get ALL actions that have sessions in this period
        // This includes archived actions if they have sessions
        val uniqueActionIds = filteredSessions.map { it.actionId }.distinct().toSet()
        
        // Fetch action titles for all unique action IDs (works for both active and archived)
        val actionTitleById = mutableMapOf<String, String>()
        uniqueActionIds.forEach { actionId ->
            val action = actionRepository.getAction(actionId)
            if (action != null) {
                actionTitleById[actionId] = action.title
            } else {
                // Fallback if action not found
                actionTitleById[actionId] = "Unknown Action"
            }
        }

        // Group sessions by action
        val sessionsByAction = filteredSessions.groupBy { it.actionId }

        // Create task insights for ALL actions that have sessions (show all actions worked on)
        val tasks = actionTitleById.map { (actionId, title) ->
            val sessions = sessionsByAction[actionId] ?: emptyList()
            val actionTotalMillis = sessions.sumOf { session ->
                if (session.endAt == null) {
                    (now.toEpochMilli() - session.startAt.toEpochMilli()).coerceAtLeast(0L)
                } else {
                    session.durationMillis.coerceAtLeast(0L)
                }
            }
            TaskInsight(
                actionId = actionId,
                actionTitle = title,
                totalMillis = actionTotalMillis,
                sessionCount = sessions.size
            )
        }.sortedByDescending { it.totalMillis }

        val topTask = tasks.firstOrNull()

        Result(
            totalMillis = totalMillis,
            topTask = topTask,
            sessionCount = filteredSessions.size,
            tasks = tasks
        )
    }
}

