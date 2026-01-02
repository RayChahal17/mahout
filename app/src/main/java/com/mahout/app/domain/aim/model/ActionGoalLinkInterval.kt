package com.mahout.app.domain.aim.model

import java.time.Instant

/**
 * Domain-friendly representation of an Action <-> Goal link interval.
 * We do NOT expose Room entities outside the data layer.
 */
data class ActionGoalLinkInterval(
    val actionId: String,
    val goalId: String,
    val linkedAt: Instant,
    val unlinkedAt: Instant?
)
