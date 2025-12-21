package com.mahout.app.domain.path.model

import java.time.Instant

/**
 * Domain model for a time Session (Path tab).
 *
 * Domain naming:
 * - Uses `id` (not sessionId) to avoid leaking DB naming into the app layer.
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
