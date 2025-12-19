package com.mahout.app.data.local.path.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mahout.app.domain.path.model.SessionSource
import java.time.Instant

/**
 * TIME logs for TIME actions.
 *
 * Red-team note:
 * - endAt can be null while timer is running (in-progress).
 * - durationMillis is stored explicitly to avoid later "recompute bugs" with DST/timezone changes.
 */
@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["actionId"]),
        Index(value = ["startAt"]),
        Index(value = ["endAt"])
    ]
)
data class SessionEntity(
    @PrimaryKey val sessionId: String,

    val actionId: String,

    val startAt: Instant,
    val endAt: Instant?,

    val durationMillis: Long,

    val source: SessionSource,

    val note: String? = null,

    val createdAt: Instant,
    val updatedAt: Instant
)
