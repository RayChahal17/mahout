package com.mahout.app.domain.aim.usecase

import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.aim.repository.ActionGoalLinkRepository
import com.mahout.app.domain.aim.repository.GoalRepository
import javax.inject.Inject

/**
 * Day 11 - Delete behavior:
 * - unlink any actions linked to this goal
 * - soft delete the goal (deletedAt set)
 *
 * IMPORTANT:
 * This does NOT delete sessions.
 * Sessions belong to Actions; we only close link intervals.
 */
class DeleteGoalUseCase @Inject constructor(
    private val goals: GoalRepository,
    private val links: ActionGoalLinkRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(goalId: String) {
        val now = timeProvider.nowInstant()

        // 1) unlink actions that point to this goal
        links.unlinkActionsForGoal(goalId, unlinkedAt = now)

        // 2) soft delete the goal
        goals.softDelete(goalId, deletedAt = now)
    }
}
