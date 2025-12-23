package com.mahout.app.domain.path.usecase.timer

import com.mahout.app.core.id.IdProvider
import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.path.model.Session
import com.mahout.app.domain.path.model.SessionSource
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.model.TimerStatus
import com.mahout.app.domain.path.repository.SessionRepository
import com.mahout.app.domain.path.repository.TimerRepository
import javax.inject.Inject

/**
 * Start a timer for the given Action.
 *
 * V1 rule:
 * - Only ONE timer can exist at a time.
 *
 * Safety behavior (red-team):
 * - If we somehow already have an in-progress session, we finalize it first.
 *   This prevents "dangling" sessions with endAt=null from breaking the day map later.
 */
class StartTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    private val sessionRepository: SessionRepository,
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

        // Persist "timer UX state" separately so we can represent PAUSED vs STOPPED.
        timerRepository.upsert(
            TimerState(
                status = TimerStatus.RUNNING,
                actionId = actionId,
                currentSessionId = sessionId,
                accumulatedMillis = 0L,
                updatedAt = now
            )
        )
    }
}
