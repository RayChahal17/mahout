package com.mahout.app.domain.aim.model

import java.time.Instant
import java.time.LocalDate

/**
 * Domain model for a Goal (Aim tab).
 * Pure Kotlin: no Room/Firestore/UI dependencies.
 */
data class Goal(
    val id: String,
    val title: String,
    val why: String?,
    val horizon: GoalHorizon,
    val status: GoalStatus,
    val priority: Int,
    val targetDate: LocalDate?,
    val parentGoalId: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
