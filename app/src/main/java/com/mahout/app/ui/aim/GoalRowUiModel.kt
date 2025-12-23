package com.mahout.app.ui.aim

import com.mahout.app.domain.aim.model.GoalHorizon
import java.time.LocalDate

/**
 * UI-friendly model for rendering a row in the Goals list.
 * Keep it small: only what the UI needs.
 */
data class GoalRowUiModel(
    val id: String,
    val title: String,
    val why: String?,
    val horizon: GoalHorizon,
    val targetDate: LocalDate?
)
