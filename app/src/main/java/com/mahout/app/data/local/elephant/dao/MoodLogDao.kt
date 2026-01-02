package com.mahout.app.data.local.elephant.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.elephant.entity.MoodLogEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodLogDao {

    @Query("SELECT * FROM mood_logs ORDER BY at DESC")
    fun observeMoodLogs(): Flow<List<MoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MoodLogEntity)

    @Query("SELECT * FROM mood_logs ORDER BY at DESC")
    suspend fun getAll(): List<MoodLogEntity>

    @Query("SELECT * FROM mood_logs WHERE at >= :from AND at <= :to ORDER BY at DESC")
    suspend fun getBetween(from: Instant, to: Instant): List<MoodLogEntity>

    /**
     * Delete a single mood log by id.
     *
     * Why do we need this?
     * - Elephant history UI needs a safe “delete with confirmation” flow.
     * - No schema change required; this is just a new DAO query.
     */
    @Query("DELETE FROM mood_logs WHERE moodLogId = :id")
    suspend fun deleteById(id: String)
}
