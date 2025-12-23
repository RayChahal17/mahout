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
 * Resume a paused timer.
 *
 * Implementation:
 * - Create a new in-progress Session (a new RUNNING segment).
 * - Update TimerState to RUNNING and store the new currentSessionId.
 */
class ResumeTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
    private val sessionRepository: SessionRepository,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() {
        val now = timeProvider.nowInstant()
        val state = timerRepository.getTimerStateOrNull() ?: return
        if (state.status != TimerStatus.PAUSED) return

        val actionId = state.actionId ?: return // can't resume if we lost the actionId

        val sessionId = idProvider.newId()
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
                currentSessionId = sessionId,
                accumulatedMillis = state.accumulatedMillis,
                updatedAt = now
            )
        )
    }
}
