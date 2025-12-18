package com.mahout.app.domain.path.model

import java.time.Instant

/**
 * A time log for a TIME action.
 * Note: endAt == null means "in progress" (critical for timer recovery).
 */
data class Session(
    val id: String,
    val actionId: String,
    val startAt: Instant,
    val endAt: Instant?,
    val durationMillis: Long,
    val source: SessionSource,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
