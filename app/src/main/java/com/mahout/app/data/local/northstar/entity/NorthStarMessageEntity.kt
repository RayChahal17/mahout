package com.mahout.app.data.local.northstar.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Stores North Star outputs (JSON-first).
 *
 * We store raw JSON so UI never breaks and we can evolve schemas without migrations.
 * This also acts like an "audit log" of what North Star produced.
 */
@Entity(
    tableName = "north_star_messages",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["trigger"]),
        Index(value = ["isDeleted"])
    ]
)
data class NorthStarMessageEntity(
    @PrimaryKey val messageId: String,

    /**
     * e.g. "MORNING_OPEN", "EVENING_OPEN", "MOOD_TRIGGER"
     * Keep as String to stay flexible.
     */
    val trigger: String,

    /**
     * Optional: the request/prompt metadata you used to generate the response.
     * (Useful for debugging; can be null.)
     */
    val requestJson: String?,

    /**
     * The JSON-first response North Star produced.
     * Even if the model fails, we can store a fallback JSON envelope.
     */
    val responseJson: String,

    val createdAt: Instant,
    val updatedAt: Instant,

    val isDeleted: Boolean = false,
    val deletedAt: Instant? = null
)
