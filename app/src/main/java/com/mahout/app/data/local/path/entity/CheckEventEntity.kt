package com.mahout.app.data.local.path.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * CHECKLIST logs for CHECKLIST actions.
 *
 * Why isChecked exists:
 * - Your UI uses toggles.
 * - Instead of mutating history, we can record both "checked" and "unchecked" events.
 * - Later queries compute the current state by looking at the most recent event per (actionId, day/week).
 */
@Entity(
    tableName = "check_events",
    indices = [
        Index(value = ["actionId"]),
        Index(value = ["occurredAt"])
    ]
)
data class CheckEventEntity(
    @PrimaryKey val checkEventId: String,

    val actionId: String,
    val occurredAt: Instant,

    val isChecked: Boolean = true,

    /**
     * Supports "did it 3 times" type actions without creating 3 rows.
     */
    val quantity: Int = 1,

    val note: String? = null
)
