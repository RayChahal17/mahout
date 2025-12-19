package com.mahout.app.data.local.aim.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.aim.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE deletedAt IS NULL ORDER BY priority DESC, updatedAt DESC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE goalId = :goalId LIMIT 1")
    suspend fun getGoal(goalId: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    /**
     * Soft delete to preserve receipts/history. UI can hide deleted goals.
     */
    @Query("UPDATE goals SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE goalId = :goalId")
    suspend fun softDelete(goalId: String, deletedAt: java.time.Instant)
}
