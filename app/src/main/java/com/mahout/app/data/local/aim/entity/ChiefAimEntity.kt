package com.mahout.app.data.local.aim.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * V1 includes Chief Aim, and it behaves like a singleton row.
 * We store it as ID=1 always, so "insert" is really "upsert".
 */
@Entity(tableName = "chief_aim")
data class ChiefAimEntity(
    @PrimaryKey val id: Int = 1,

    val title: String,
    val description: String?,

    /**
     * Optional “Target” date shown on the Chief Aim hero card.
     * Stored as LocalDate (no timezone) because it’s a human “calendar date”.
     *
     * Room will store this as TEXT via MahoutTypeConverters.localDateToIso().
     */
    val targetDate: LocalDate?,

    val createdAt: Instant,
    val updatedAt: Instant
)
