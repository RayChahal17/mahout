package com.mahout.app.data.local.aim.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mahout.app.domain.path.model.ActionCadence
import com.mahout.app.domain.path.model.ActionTrackingType
import java.time.Instant

/**
 * V1 scope lock:
 * Actions are TIME-tracked only (Sessions).
 */

@Entity(
    tableName = "actions",
    indices = [
        Index(value = ["trackingType"]),
        Index(value = ["cadence"]),
        Index(value = ["isArchived"])
    ]
)
data class ActionEntity(
    @PrimaryKey val actionId: String,

    val title: String,
    val description: String?,

    val trackingType: ActionTrackingType, // TIME
    val cadence: ActionCadence,

    /**
     * Optional target (V1):
     * - TIME: minutes target
     *
     * (Checklist tracking is deferred.)
     */
    val targetValue: Int?,

    val isArchived: Boolean = false,

    val createdAt: Instant,
    val updatedAt: Instant,

    val archivedAt: Instant? = null
)
