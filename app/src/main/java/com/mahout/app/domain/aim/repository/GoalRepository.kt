package com.mahout.app.domain.aim.repository

import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Domain-layer contract for Goals.
 *
 * BACKWARDS COMPATIBILITY NOTE:
 * -----------------------------
 * You already had code (ArchiveGoalUseCase) calling:
 *    repo.updateStatus(goalId, status, updatedAt)
 *
 * When we introduced "archiveGoal()", we should NOT have removed updateStatus().
 * Removing methods from an interface breaks compilation across the project.
 *
 * So we keep BOTH:
 * - updateStatus(...) => generic status updates (older code uses this)
 * - archiveGoal(...)  => convenience helper for ARCHIVED behavior (newer code can use this)
 */
interface GoalRepository {

    /**
     * Observe ALL non-deleted goals (includes archived).
     * Needed for Aim screen buckets (including Archived).
     */
    fun observeGoals(): Flow<List<Goal>>

    /**
     * Observe only non-archived goals.
     * Useful for "Active goals" views / usecases.
     */
    fun observeActiveGoals(): Flow<List<Goal>>

    /**
     * Fetch a single goal (nullable if not found).
     */
    suspend fun getGoal(goalId: String): Goal?

    /**
     * Insert or update the goal.
     */
    suspend fun upsert(goal: Goal)

    /**
     * Generic status update (used by existing code like ArchiveGoalUseCase).
     */
    suspend fun updateStatus(
        goalId: String,
        status: GoalStatus,
        updatedAt: Instant
    )

    /**
     * Convenience method for archiving.
     * Newer code may call this directly.
     *
     * We keep it to avoid repeating the ARCHIVED constant everywhere.
     */
    suspend fun archiveGoal(
        goalId: String,
        updatedAt: Instant
    ) = updateStatus(goalId, GoalStatus.ARCHIVED, updatedAt)
}
