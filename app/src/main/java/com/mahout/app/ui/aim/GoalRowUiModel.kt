// app/src/main/java/com/mahout/app/ui/aim/GoalRowUiModel.kt
package com.mahout.app.ui.aim

import com.mahout.app.domain.aim.model.GoalHorizon
import java.time.LocalDate

/**
 * UI model for a Goal row in the Aim roadmap list.
 *
 * IMPORTANT:
 * - Keep this model "stable" so AimFragment and AimViewModel don't break
 *   when domain models evolve.
 *
 * Day 11:
 * - We show title/why/horizon/targetDate in UI.
 *
 * Day 11 (forward-compatible):
 * - parentGoalId exists in DB/domain for laddering,
 *   but UI doesn't need it yet (so it's optional).
 */
data class GoalRowUiModel(
    val id: String,
    val title: String,
    val why: String?,
    val horizon: GoalHorizon,

    // Used for: auto-horizon, labels, and date-bound UI messaging.
    val targetDate: LocalDate?,

    // Optional laddering (not wired in UI yet).
    val parentGoalId: String? = null
)
