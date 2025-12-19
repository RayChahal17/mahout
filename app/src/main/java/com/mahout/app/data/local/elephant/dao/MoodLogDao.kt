package com.mahout.app.data.local.elephant.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.elephant.entity.MoodLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodLogDao {

    @Query("SELECT * FROM mood_logs ORDER BY at DESC")
    fun observeMoodLogs(): Flow<List<MoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MoodLogEntity)
}
