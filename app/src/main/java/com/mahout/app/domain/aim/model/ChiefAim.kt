package com.mahout.app.domain.aim.model

import java.time.Instant
import java.time.LocalDate

/**
 * Domain model for Chief Aim (singleton record in V1).
 */
data class ChiefAim(
    val title: String,
    val description: String?,
    val targetDate: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant
)
