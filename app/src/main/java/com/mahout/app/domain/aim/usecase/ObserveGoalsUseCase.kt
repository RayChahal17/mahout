package com.mahout.app.domain.aim.usecase

import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns ALL non-deleted goals (including archived).
 * The ViewModel decides what to show per bucket.
 */
class ObserveGoalsUseCase @Inject constructor(
    private val repo: GoalRepository
) {
    operator fun invoke(): Flow<List<Goal>> = repo.observeGoals()
}
