package com.mahout.app.domain.northstar.model

import java.time.Instant

/**
 * Singleton-style record. We keep JSON in V1 to avoid schema churn.
 */
data class FutureProfile(
    val id: Int = 1,
    val profileJson: String,
    val kvSummaryJson: String,
    val updatedAt: Instant
)
