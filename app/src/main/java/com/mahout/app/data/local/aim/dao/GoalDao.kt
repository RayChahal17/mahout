package com.mahout.app.data.local.aim.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.aim.entity.GoalEntity
import com.mahout.app.domain.aim.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface GoalDao {

    /**
     * All non-deleted goals (includes ARCHIVED).
     * Useful for admin/debug or future screens.
     */
    @Query("SELECT * FROM goals WHERE deletedAt IS NULL ORDER BY priority DESC, updatedAt DESC")
    fun observeGoals(): Flow<List<GoalEntity>>

    /**
     * Day 11: active goals list for Aim tab.
     * - Excludes soft-deleted goals
     * - Excludes ARCHIVED goals (they should not appear in the active roadmap list)
     *
     * Note: GoalStatus is stored as TEXT via TypeConverters using enum.name,
     * so comparing against 'ARCHIVED' works.
     */
    @Query("SELECT * FROM goals WHERE deletedAt IS NULL AND status != 'ARCHIVED' ORDER BY priority DESC, updatedAt DESC")
    fun observeActiveGoals(): Flow<List<GoalEntity>>

    /**
     * Observe a single goal (already useful for future linking screens).
     */
    @Query("SELECT * FROM goals WHERE goalId = :goalId LIMIT 1")
    fun observeGoal(goalId: String): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE goalId = :goalId LIMIT 1")
    suspend fun getGoal(goalId: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    /**
     * Update status (ACTIVE/ACHIEVED/ARCHIVED) without deleting the record.
     * We also touch updatedAt to keep ordering consistent.
     */
    @Query("UPDATE goals SET status = :status, updatedAt = :updatedAt WHERE goalId = :goalId")
    suspend fun updateStatus(goalId: String, status: GoalStatus, updatedAt: Instant)

    /**
     * Soft delete to preserve receipts/history. UI can hide deleted goals.
     */
    @Query("UPDATE goals SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE goalId = :goalId")
    suspend fun softDelete(goalId: String, deletedAt: Instant)
}
