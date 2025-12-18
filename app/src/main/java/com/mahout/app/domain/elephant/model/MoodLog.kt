package com.mahout.app.domain.elephant.model

import java.time.Instant

/**
 * Mood log entry (Elephant tab).
 * Keep 'feeling' as String in V1 to stay flexible.
 */
data class MoodLog(
    val id: String,
    val feeling: String,
    val intensity: Int?,
    val note: String?,
    val at: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)
