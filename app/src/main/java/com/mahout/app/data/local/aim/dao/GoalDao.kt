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
 * - Primary key is goalId
 * - deletedAt is soft-delete flag
 */
@Dao
interface GoalDao {

    @Query(
        """
        SELECT * FROM goals
        WHERE deletedAt IS NULL
        ORDER BY priority ASC
        """
    )
    fun observeGoals(): Flow<List<GoalEntity>>

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

    @Query(
        """
        SELECT * FROM goals
        WHERE goalId = :goalId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    fun observeGoal(goalId: String): Flow<GoalEntity?>

    @Query(
        """
        SELECT * FROM goals
        WHERE goalId = :goalId
          AND deletedAt IS NULL
        LIMIT 1
        """
    )
    suspend fun getById(goalId: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GoalEntity)

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

    /**
     * Day 11 - Delete behavior:
     * Soft-delete the goal so it disappears from UI lists,
     * while keeping the record for safety/history.
     */
    @Query(
        """
        UPDATE goals
        SET deletedAt = :deletedAt,
            updatedAt = :deletedAt
        WHERE goalId = :goalId
        """
    )
    suspend fun softDelete(goalId: String, deletedAt: Instant)
}
