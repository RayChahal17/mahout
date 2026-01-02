package com.mahout.app.ui.aim

import com.mahout.app.domain.aim.model.GoalHorizon
import java.time.LocalDate

data class GoalRowUiModel(
    val id: String,
    val title: String,
    val why: String?,
    val horizon: GoalHorizon,
    val targetDate: LocalDate?,
    val parentGoalId: String?,
    val linkedSummary: String = "",
    val linkedCount: Int = 0,
    // Weekly stats for goal cards
    val weeklyTimeMillis: Long = 0L,
    val weeklySessionCount: Int = 0,
    val activeDays: Int = 0,
    val lastTouchedDate: LocalDate? = null,
    val progressPercent: Int = 0 // Weekly consistency progress (0-100)
)
