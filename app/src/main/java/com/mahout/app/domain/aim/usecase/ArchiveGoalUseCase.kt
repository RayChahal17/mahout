package com.mahout.app.domain.aim.usecase

import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.aim.model.GoalStatus
import com.mahout.app.domain.aim.repository.GoalRepository
import javax.inject.Inject

class ArchiveGoalUseCase @Inject constructor(
    private val repo: GoalRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(goalId: String) {
        repo.updateStatus(
            goalId = goalId,
            status = GoalStatus.ARCHIVED,
            updatedAt = timeProvider.nowInstant()
        )
    }
}
