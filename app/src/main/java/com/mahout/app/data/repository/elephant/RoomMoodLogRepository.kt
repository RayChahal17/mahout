package com.mahout.app.data.repository.elephant

import com.mahout.app.data.local.elephant.dao.MoodLogDao
import com.mahout.app.data.mapper.elephant.toDomain
import com.mahout.app.data.mapper.elephant.toEntity
import com.mahout.app.domain.elephant.model.MoodLog
import com.mahout.app.domain.elephant.repository.MoodLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

/**
 * Room-backed implementation of MoodLogRepository.
 *
 * Offline-first rule:
 * - Room is the source of truth.
 * - UI observes Room flows and renders them.
 */
class RoomMoodLogRepository @Inject constructor(
    private val dao: MoodLogDao
) : MoodLogRepository {

    override fun observeMoodLogs(): Flow<List<MoodLog>> =
        dao.observeMoodLogs().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<MoodLog> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getBetween(from: Instant, to: Instant): List<MoodLog> =
        dao.getBetween(from, to).map { it.toDomain() }

    override suspend fun upsert(log: MoodLog) {
        dao.upsert(log.toEntity())
    }

    override suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }
}

