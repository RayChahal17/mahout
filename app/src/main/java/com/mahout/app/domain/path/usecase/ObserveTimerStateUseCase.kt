package com.mahout.app.domain.path.usecase

import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.repository.TimerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observe the singleton Path timer state as a Flow.
 *
 * Why a UseCase wrapper?
 * - Keeps UI/Service code from depending directly on repositories
 * - Keeps conventions consistent with the rest of the codebase (ArchiveActionUseCase etc.)
 */
class ObserveTimerStateUseCase @Inject constructor(
    private val timerRepository: TimerRepository
) {
    operator fun invoke(): Flow<TimerState> = timerRepository.observeTimerState()
}
