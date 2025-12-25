package com.mahout.app.domain.path.usecase.timer

import com.mahout.app.domain.path.model.TimerState
import com.mahout.app.domain.path.repository.TimerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTimerStateUseCase @Inject constructor(
    private val timerRepository: TimerRepository
) {
    operator fun invoke(): Flow<TimerState> = timerRepository.observeTimerState()
}
