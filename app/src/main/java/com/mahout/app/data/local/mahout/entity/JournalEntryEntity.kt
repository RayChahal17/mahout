package com.mahout.app.data.local.mahout.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mahout.app.domain.mahout.model.JournalEntryType
import java.time.Instant

/**
 * Mahout journal entries.
 */
@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["type"]),
        Index(value = ["createdAt"])
    ]
)
data class JournalEntryEntity(
    @PrimaryKey val journalEntryId: String,

    val type: JournalEntryType,

    val title: String?,
    val body: String,

    val promptId: String?,
    val relatedMoodLogId: String?,

    val createdAt: Instant,
    val updatedAt: Instant,

    val deletedAt: Instant? = null
)