package com.mahout.app.domain.aim.usecase

import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.aim.model.ActionGoalLinkInterval
import com.mahout.app.domain.aim.repository.ActionGoalLinkRepository
import com.mahout.app.domain.path.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class GetGoalWeeklyReceiptsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val actionGoalLinkRepository: ActionGoalLinkRepository,
    private val timeProvider: TimeProvider
) {

    data class Receipt(
        val actionId: String,
        val startAt: Instant,
        val durationMillis: Long
    )

    data class Result(
        val receipts: List<Receipt>,
        val perActionWeekMillis: Map<String, Long>
    )

    suspend operator fun invoke(goalId: String): Result = withContext(Dispatchers.Default) {
        val zone = ZoneId.systemDefault()
        val now = timeProvider.nowInstant().atZone(zone).toLocalDate()

        val weekStartLocal = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStart = weekStartLocal.atStartOfDay(zone).toInstant()
        val weekEnd = weekStart.plus(7, ChronoUnit.DAYS)

        val sessions = sessionRepository.getSessionsInRange(weekStart, weekEnd)
        val links = actionGoalLinkRepository.getLinksOverlappingForGoal(goalId, weekStart, weekEnd)

        val intervalsByAction: Map<String, List<ActionGoalLinkInterval>> =
            links.groupBy { it.actionId }

        fun isLinkedAt(actionId: String, at: Instant): Boolean {
            val intervals = intervalsByAction[actionId] ?: return false
            return intervals.any { it.linkedAt <= at && (it.unlinkedAt == null || it.unlinkedAt > at) }
        }

        val matched = sessions
            .asSequence()
            .filter { isLinkedAt(it.actionId, it.startAt) }
            .map {
                Receipt(
                    actionId = it.actionId,
                    startAt = it.startAt,
                    durationMillis = it.durationMillis
                )
            }
            .toList()

        val perAction = matched
            .groupBy { it.actionId }
            .mapValues { (_, rs) -> rs.sumOf { it.durationMillis } }

        Result(receipts = matched, perActionWeekMillis = perAction)
    }
}
