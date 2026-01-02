package com.mahout.app.domain.path.usecase.timer

import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.model.TimerStatus
import com.mahout.app.domain.path.repository.SessionRepository
import com.mahout.app.domain.path.repository.TimerRepository
import javax.inject.Inject

/**
 * Pause the running timer.
 *
 * Implementation:
 * - Finalize the current in-progress Session row (set endAt + durationMillis).
 * - Update TimerState to PAUSED and accumulate the segment duration into accumulatedMillis.
 *
 * This produces "one Session per RUNNING segment". It's reliable and easy to reason about.
 */
class PauseTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    private val sessionRepository: SessionRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() {
        val now = timeProvider.nowInstant()
        val state = timerRepository.getTimerStateOrNull() ?: return
        if (state.status != TimerStatus.RUNNING) return

        // Finalize in-progress session if present.
        val inProgress = sessionRepository.getInProgressSession()
        val segmentMillis = if (inProgress != null) {
            val duration = (now.toEpochMilli() - inProgress.startAt.toEpochMilli()).coerceAtLeast(0L)
            val closed = inProgress.copy(
                endAt = now,
                durationMillis = duration,
                updatedAt = now
            )
            sessionRepository.upsert(closed)
            duration
        } else {
            0L
        }

        timerRepository.upsert(
            TimerState(
                status = TimerStatus.PAUSED,
                actionId = state.actionId,
                accumulatedMillis = state.accumulatedMillis + segmentMillis,
                updatedAt = now
            )
        )
    }
}
