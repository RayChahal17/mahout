package com.mahout.app.domain.path.model

import java.time.Instant

/**
 * Domain model for an Action (Path tab).
 *
 * Domain naming:
 * - Uses `id` (not actionId) to avoid leaking DB naming into the app layer.
 */
data class Action(
    val id: String,
    val title: String,
    val description: String?,
    val trackingType: ActionTrackingType,
    val cadence: ActionCadence,

    /**
     * Optional target (V1):
     * - TIME: minutes target
     *
     * (Checklist tracking is deferred.)
     */
    val targetValue: Int?,


    /**
     * Archived actions are hidden by default but kept for history/receipts.
     */
    val isArchived: Boolean,

    val createdAt: Instant,
    val updatedAt: Instant,

    /**
     * When this action was archived (null if not archived / unknown).
     * Default keeps old call sites compiling.
     */
    val archivedAt: Instant? = null
)
