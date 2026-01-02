package com.mahout.app.data.mapper.mahout

import com.mahout.app.data.local.mahout.entity.JournalEntryEntity
import com.mahout.app.domain.mahout.model.JournalEntry

/**
 * “Mapper” functions keep Room entities out of UI/domain layers.
 * This is the exact pattern you already use in Elephant/Path.
 */

fun JournalEntryEntity.toDomain(): JournalEntry =
    JournalEntry(
        id = journalEntryId,
        type = type,
        title = title,
        body = body,
        promptId = promptId,
        relatedMoodLogId = relatedMoodLogId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun JournalEntry.toEntity(): JournalEntryEntity =
    JournalEntryEntity(
        journalEntryId = id,
        type = type,
        title = title,
        body = body,
        promptId = promptId,
        relatedMoodLogId = relatedMoodLogId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = null // upsert is always “alive” again
    )
