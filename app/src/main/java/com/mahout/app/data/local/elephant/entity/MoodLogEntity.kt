package com.mahout.app.data.local.elephant.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Elephant mood logs.
 */
@Entity(
    tableName = "mood_logs",
    indices = [
        Index(value = ["at"])
    ]
)
data class MoodLogEntity(
    @PrimaryKey val moodLogId: String,

    val feeling: String,
    val intensity: Int?,
    val note: String?,

    val at: Instant,

    val createdAt: Instant,
    val updatedAt: Instant
)
