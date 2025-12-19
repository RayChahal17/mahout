package com.mahout.app.data.local.path.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.path.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions WHERE actionId = :actionId ORDER BY startAt DESC")
    fun observeSessionsForAction(actionId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE endAt IS NULL ORDER BY startAt DESC LIMIT 1")
    suspend fun getInProgressSession(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE sessionId = :sessionId")
    suspend fun delete(sessionId: String)

    @Query("SELECT * FROM sessions WHERE startAt BETWEEN :from AND :to ORDER BY startAt ASC")
    suspend fun getSessionsInRange(from: Instant, to: Instant): List<SessionEntity>
}
