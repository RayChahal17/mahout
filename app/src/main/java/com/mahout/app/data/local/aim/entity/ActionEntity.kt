package com.mahout.app.data.local.aim.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mahout.app.domain.path.model.ActionCadence
import com.mahout.app.domain.path.model.ActionTrackingType
import java.time.Instant

/**
 * Actions are the "bridge object".
 * A single Action can be tracked by TIME (Sessions) or CHECKLIST (CheckEvents).
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

    val trackingType: ActionTrackingType, // TIME or CHECKLIST
    val cadence: ActionCadence,

    /**
     * Optional target.
     * - TIME: minutes target
     * - CHECKLIST: count target
     */
    val targetValue: Int?,

    val isArchived: Boolean = false,

    val createdAt: Instant,
    val updatedAt: Instant,

    val archivedAt: Instant? = null
)
