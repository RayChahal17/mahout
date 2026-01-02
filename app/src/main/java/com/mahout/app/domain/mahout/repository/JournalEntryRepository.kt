package com.mahout.app.domain.mahout.repository

import com.mahout.app.domain.mahout.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Domain contract for Mahout journaling.
 *
 * IMPORTANT:
 * - Room is the source of truth in V1 (offline-first).
 * - We soft-delete via deletedAt.
 */
interface JournalEntryRepository {

    /** Reverse chronological stream of non-deleted entries */
    fun observeEntries(): Flow<List<JournalEntry>>

    /** Used for “tap list row -> open editor to edit existing entry” */
    suspend fun getById(id: String): JournalEntry?

    /** Insert OR update (Room REPLACE strategy) */
    suspend fun upsert(entry: JournalEntry)

    /** Soft delete so we can restore later if needed */
    suspend fun softDelete(id: String, deletedAt: Instant)
}
