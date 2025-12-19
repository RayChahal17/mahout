package com.mahout.app.data.local.path.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.path.entity.CheckEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CheckEventDao {

    @Query("SELECT * FROM check_events WHERE actionId = :actionId ORDER BY occurredAt DESC")
    fun observeCheckEventsForAction(actionId: String): Flow<List<CheckEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CheckEventEntity)

    @Query("DELETE FROM check_events WHERE checkEventId = :checkEventId")
    suspend fun delete(checkEventId: String)

    @Query("SELECT * FROM check_events WHERE occurredAt BETWEEN :from AND :to ORDER BY occurredAt ASC")
    suspend fun getCheckEventsInRange(from: Instant, to: Instant): List<CheckEventEntity>
}
