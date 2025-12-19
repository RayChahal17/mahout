package com.mahout.app.data.local.aim.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * V1 includes Chief Aim, and it behaves like a singleton row.
 * We store it as ID=1 always, so "insert" is really "upsert".
 */
@Entity(tableName = "chief_aim")
data class ChiefAimEntity(
    @PrimaryKey val id: Int = 1,

    val title: String,
    val description: String?,

    val createdAt: Instant,
    val updatedAt: Instant
)
