package com.mahout.app.domain.elephant.repository

import com.mahout.app.domain.elephant.model.MoodLog
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Domain-level contract for mood logs (Elephant tab).
 *
 * V1 is offline-first, so Room is the source of truth. This interface keeps UI/domain
 * layers unaware of Room, SQL, etc.
 */
interface MoodLogRepository {
    fun observeMoodLogs(): Flow<List<MoodLog>>
    suspend fun getAll(): List<MoodLog>
    suspend fun getBetween(from: Instant, to: Instant): List<MoodLog>
    suspend fun upsert(log: MoodLog)

    /**
     * Delete a single log by id (used by the Elephant “history” list).
     *
     * Note: This is a behavior change only (no schema migration).
     */
    suspend fun deleteById(id: String)
}

