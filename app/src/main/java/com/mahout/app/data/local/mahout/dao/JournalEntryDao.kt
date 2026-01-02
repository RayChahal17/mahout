package com.mahout.app.data.local.mahout.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahout.app.data.local.mahout.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface JournalEntryDao {

    /**
     * Reverse chronological feed.
     * We soft-delete by setting deletedAt (so we can undo later / debug).
     */
    @Query("SELECT * FROM journal_entries WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeEntries(): Flow<List<JournalEntryEntity>>

    /**
     * Needed for “tap entry → open editor to edit existing”.
     * LIMIT 1 avoids surprises if data ever gets corrupted.
     */
    @Query("SELECT * FROM journal_entries WHERE journalEntryId = :id LIMIT 1")
    suspend fun getById(id: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: JournalEntryEntity)

    @Query("UPDATE journal_entries SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE journalEntryId = :id")
    suspend fun softDelete(id: String, deletedAt: Instant)


}
