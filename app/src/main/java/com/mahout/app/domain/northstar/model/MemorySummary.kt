package com.mahout.app.domain.northstar.model

import java.time.Instant
import java.time.LocalDate

/**
 * Memory summary used by North Star. Soft delete prevents accidental data loss.
 */
data class MemorySummary(
    val id: String,
    val periodType: MemoryPeriodType,
    val periodStart: LocalDate,
    val narrative: String,
    val statsJson: String?,
    val futureYouNote: String?,
    val isDeleted: Boolean,
    val updatedAt: Instant
)
