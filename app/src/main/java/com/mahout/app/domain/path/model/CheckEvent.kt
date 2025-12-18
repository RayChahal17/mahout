package com.mahout.app.domain.path.model

import java.time.Instant

/**
 * Completion event for CHECK actions.
 */
data class CheckEvent(
    val id: String,
    val actionId: String,
    val at: Instant,
    val quantity: Int,
    val note: String?
)
