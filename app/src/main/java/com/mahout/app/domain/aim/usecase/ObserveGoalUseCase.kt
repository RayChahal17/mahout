package com.mahout.app.domain.aim.usecase

import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observe a single goal (or null if deleted/not found).
 * Used by Goal Detail screen.
 */
class ObserveGoalUseCase @Inject constructor(
    private val repo: GoalRepository
) {
    operator fun invoke(goalId: String): Flow<Goal?> = repo.observeGoal(goalId)
}
