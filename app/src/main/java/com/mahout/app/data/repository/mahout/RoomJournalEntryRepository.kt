package com.mahout.app.data.repository.mahout

import com.mahout.app.data.local.mahout.dao.JournalEntryDao
import com.mahout.app.data.mapper.mahout.toDomain
import com.mahout.app.data.mapper.mahout.toEntity
import com.mahout.app.domain.mahout.model.JournalEntry
import com.mahout.app.domain.mahout.repository.JournalEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomJournalEntryRepository @Inject constructor(
    private val dao: JournalEntryDao
) : JournalEntryRepository {

    override fun observeEntries(): Flow<List<JournalEntry>> =
        dao.observeEntries().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): JournalEntry? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(entry: JournalEntry) {
        dao.upsert(entry.toEntity())
    }

    override suspend fun softDelete(id: String, deletedAt: Instant) {
        dao.softDelete(id, deletedAt)
    }
}
