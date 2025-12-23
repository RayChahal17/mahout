package com.mahout.app.data.local.aim.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.aim.entity.GoalEntity
import com.mahout.app.domain.aim.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Room DAO for the "goals" table.
 *
 * Key points:
 * - Your primary key column is "goalId" (NOT "id").
 * - We use deletedAt as a soft-delete flag. Anything deletedAt != null is treated as "gone".
 * - We provide BOTH:
 *   1) observeGoals() -> list including archived
 *   2) observeActiveGoals() -> list excluding archived
 *   3) observeGoal(goalId) -> Flow for a single goal (needed by Action↔Goal link feature)
 */
@Dao
interface GoalDao {

    /**
     * Observe ALL non-deleted goals (includes archived).
     * Used by Aim bucket filtering (including Archived bucket).
     */
    @Query(
        """
        SELECT * FROM goals
        WHERE deletedAt IS NULL
        ORDER BY priority ASC
        """
    )
    fun observeGoals(): Flow<List<GoalEntity>>

    /**
     * Observe only non-archived goals.
     * Useful for earlier Day 11 "active goals" list flows.
     */
    @Query(
        """
        SELECT * FROM goals
        WHERE deletedAt IS NULL
          AND status != :archivedStatus
        ORDER BY priority ASC
        """
    )
    fun observeActiveGoals(
        archivedStatus: GoalStatus = GoalStatus.ARCHIVED
    ): Flow<List<GoalEntity>>

    /**
     * Observe a SINGLE goal by id as a Flow.
     *
     * Why do we need this?
     * RoomActionGoalLinkRepository observes an ActionGoalLink (Flow),
     * then "switches" to the linked Goal (Flow) using flatMapLatest.
     *
     * If a goal is soft-deleted, we return null so UI can treat it as unlinked/missing.
     */
    @Query(
        """
        SELECT * FROM goals
        WHERE goalId = :goalId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    fun observeGoal(goalId: String): Flow<GoalEntity?>

    /**
     * One-shot fetch of a goal (nullable).
     * We also filter soft-deleted rows here to keep behavior consistent.
     */
    @Query(
        """
        SELECT * FROM goals
        WHERE goalId = :goalId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    suspend fun getById(goalId: String): GoalEntity?

    /**
     * Upsert using REPLACE so primary key overwrites.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GoalEntity)

    /**
     * Update status (used for archiving).
     */
    @Query(
        """
        UPDATE goals
        SET status = :newStatus,
            updatedAt = :updatedAt
        WHERE goalId = :goalId
        """
    )
    suspend fun updateStatus(
        goalId: String,
        newStatus: GoalStatus,
        updatedAt: Instant
    )
}
