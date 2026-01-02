package com.mahout.app.domain.path.usecase.timer

import com.mahout.app.core.id.IdProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.path.model.ActionCadence
import com.mahout.app.domain.path.model.Session
import com.mahout.app.domain.path.model.SessionSource
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.model.TimerStatus
import com.mahout.app.domain.path.repository.ActionRepository
import com.mahout.app.domain.path.repository.SessionRepository
import com.mahout.app.domain.path.repository.TimerRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Start a timer for the given Action.
 *
 * Day 13 upgrade:
 * - accumulatedMillis is seeded from persisted Sessions for the correct cadence window:
 *   DAILY  -> today's total
 *   WEEKLY -> this week (Mon..Sun)
 *   ONE_TIME -> lifetime total
 */
class StartTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    private val sessionRepository: SessionRepository,
    private val actionRepository: ActionRepository,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(actionId: String) {
        val now = timeProvider.nowInstant()

        // Defensive cleanup: if we have an in-progress session, close it.
        sessionRepository.getInProgressSession()?.let { inProgress ->
            val closed = inProgress.copy(
                endAt = now,
                durationMillis = (now.toEpochMilli() - inProgress.startAt.toEpochMilli()).coerceAtLeast(0L),
                updatedAt = now
            )
            sessionRepository.upsert(closed)
        }

        // Seed from historical sessions so UI + notification continue from correct total.
        val cadence = actionRepository.getAction(actionId)?.cadence ?: ActionCadence.DAILY
        val seededMillis = computeSeededMillis(actionId, cadence, now)

        val sessionId = idProvider.newId()

        // Create an in-progress session row immediately so we're durable.
        val inProgressSession = Session(
            id = sessionId,
            actionId = actionId,
            startAt = now,
            endAt = null,
            durationMillis = 0L,
            source = SessionSource.TIMER,
            note = null,
            createdAt = now,
            updatedAt = now
        )
        sessionRepository.upsert(inProgressSession)

        timerRepository.upsert(
            TimerState(
                status = TimerStatus.RUNNING,
                actionId = actionId,
                accumulatedMillis = seededMillis,
                updatedAt = now
            )
        )
    }

    private suspend fun computeSeededMillis(actionId: String, cadence: ActionCadence, now: Instant): Long {
        val zone = ZoneId.systemDefault()
        val nowPlus = now.plusSeconds(1)

        val from: Instant = when (cadence) {
            ActionCadence.DAILY -> now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
            ActionCadence.WEEKLY -> now.atZone(zone).toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(zone)
                .toInstant()
            ActionCadence.ONE_TIME -> Instant.EPOCH
        }

        val sessions = sessionRepository.getSessionsInRange(from, nowPlus)
            .filter { it.actionId == actionId }

        return sessions.sumOf { s ->
            if (s.endAt != null) s.durationMillis.coerceAtLeast(0L)
            else (now.toEpochMilli() - s.startAt.toEpochMilli()).coerceAtLeast(0L)
        }
    }
}
