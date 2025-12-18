package com.mahout.app.domain.mahout.model

import java.time.Instant

/**
 * Journal entry (Mahout tab).
 */
data class JournalEntry(
    val id: String,
    val type: JournalEntryType,
    val title: String?,
    val body: String,
    val promptId: String?,
    val relatedMoodLogId: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
