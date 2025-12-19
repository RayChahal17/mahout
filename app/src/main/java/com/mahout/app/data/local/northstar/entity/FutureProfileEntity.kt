package com.mahout.app.data.local.northstar.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Singleton-like future profile row.
 * We keep it JSON in V1 to stay flexible as the model evolves.
 */
@Entity(tableName = "future_profile")
data class FutureProfileEntity(
    @PrimaryKey val id: Int = 1,

    val profileJson: String,
    val kvSummaryJson: String,

    val updatedAt: Instant
)
