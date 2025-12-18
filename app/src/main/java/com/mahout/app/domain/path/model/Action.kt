package com.mahout.app.domain.path.model

import java.time.Instant

/**
 * Domain model for an Action (Path tab).
 */
data class Action(
    val id: String,
    val title: String,
    val description: String?,
    val trackingType: ActionTrackingType,
    val cadence: ActionCadence,

    /**
     * Optional target:
     * - TIME: minutes target
     * - CHECK: count target
     */
    val targetValue: Int?,

    /**
     * Archived actions are hidden by default but kept for history/receipts.
     */
    val isArchived: Boolean,

    val createdAt: Instant,
    val updatedAt: Instant
)
