package com.mahout.app.domain.aim.usecase

import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveActiveGoalsUseCase @Inject constructor(
    private val repo: GoalRepository
) {
    operator fun invoke(): Flow<List<Goal>> = repo.observeActiveGoals()
}
