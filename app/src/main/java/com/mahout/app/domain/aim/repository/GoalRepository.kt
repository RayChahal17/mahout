package com.mahout.app.domain.aim.repository

import com.mahout.app.domain.aim.model.Goal
import com.mahout.app.domain.aim.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Domain-layer contract for Goals.
 *
 * Backwards compatibility:
 * - keep updateStatus(...) because older code calls it
 */
interface GoalRepository {

    fun observeGoals(): Flow<List<Goal>>
    fun observeActiveGoals(): Flow<List<Goal>>

    /**
     * Observe a single goal (or null if deleted/not found).
     * Used by Goal Detail screen.
     */
    fun observeGoal(goalId: String): Flow<Goal?>

    suspend fun getGoal(goalId: String): Goal?
    suspend fun upsert(goal: Goal)

    suspend fun updateStatus(
        goalId: String,
        status: GoalStatus,
        updatedAt: Instant
    )

    suspend fun archiveGoal(
        goalId: String,
        updatedAt: Instant
    ) = updateStatus(goalId, GoalStatus.ARCHIVED, updatedAt)

    /**
     * Day 11 - Delete behavior (soft delete).
     */
    suspend fun softDelete(goalId: String, deletedAt: Instant)
}
