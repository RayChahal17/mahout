package com.mahout.app.data.local.path.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.path.entity.TimerStateEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the singleton timer row.
 */
@Dao
interface TimerStateDao {

    @Query("SELECT * FROM timer_state WHERE timerId = 'timer' LIMIT 1")
    fun observeTimerState(): Flow<TimerStateEntity?>

    @Query("SELECT * FROM timer_state WHERE timerId = 'timer' LIMIT 1")
    suspend fun getTimerStateOrNull(): TimerStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: TimerStateEntity)

    @Query("DELETE FROM timer_state")
    suspend fun clear()
}
