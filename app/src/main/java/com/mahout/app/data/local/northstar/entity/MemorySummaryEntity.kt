package com.mahout.app.data.local.northstar.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mahout.app.domain.northstar.model.MemoryPeriodType
import java.time.Instant
import java.time.LocalDate

/**
 * Stores DAILY/MONTHLY/YEARLY memory summaries.
 * We store both:
 * - narrative (for humans)
 * - aiSummaryJson (compact key-value summary for AI prompts)
 */
@Entity(
    tableName = "memory_summaries",
    indices = [
        Index(value = ["periodType"]),
        Index(value = ["periodStart"]),
        Index(value = ["isDeleted"])
    ]
)
data class MemorySummaryEntity(
    @PrimaryKey val memoryId: String,

    val periodType: MemoryPeriodType,
    val periodStart: LocalDate,

    val narrative: String,

    val aiSummaryJson: String? = null,
    val statsJson: String? = null,
    val futureYouNote: String? = null,

    val isDeleted: Boolean = false,
    val deletedAt: Instant? = null,

    val updatedAt: Instant
)
