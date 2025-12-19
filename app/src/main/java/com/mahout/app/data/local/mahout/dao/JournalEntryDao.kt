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

    @Query("SELECT * FROM journal_entries WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeEntries(): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: JournalEntryEntity)

    @Query("UPDATE journal_entries SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE journalEntryId = :id")
    suspend fun softDelete(id: String, deletedAt: Instant)
}
