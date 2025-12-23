package com.mahout.app.domain.path.usecase.timer

import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.path.repository.SessionRepository
import com.mahout.app.domain.path.repository.TimerRepository
import javax.inject.Inject

/**
 * Stop the timer (whether RUNNING or PAUSED).
 *
 * Behavior:
 * - If there's an in-progress session, finalize it.
 * - Then clear the timer state row (STOPPED).
 */
class StopTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    private val sessionRepository: SessionRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() {
        val now = timeProvider.nowInstant()

        // If we have a dangling in-progress session (endAt=null), close it.
        sessionRepository.getInProgressSession()?.let { inProgress ->
            val duration = (now.toEpochMilli() - inProgress.startAt.toEpochMilli()).coerceAtLeast(0L)
            val closed = inProgress.copy(
                endAt = now,
                durationMillis = duration,
                updatedAt = now
            )
            sessionRepository.upsert(closed)
        }

        timerRepository.clear()
    }
}
