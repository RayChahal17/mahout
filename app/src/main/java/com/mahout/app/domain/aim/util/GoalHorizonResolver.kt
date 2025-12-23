package com.mahout.app.domain.aim.util

import com.mahout.app.domain.aim.model.GoalHorizon
import java.time.LocalDate

/**
 * Converts (today, targetDate) => GoalHorizon.
 *
 * Why this exists:
 * - Users set a target date.
 * - The app should auto-derive horizon so the label always matches the date.
 * - This prevents bugs like "1–6 months" showing even though target date is > 6 months.
 *
 * NOTE:
 * We keep it as a pure function (no Android deps).
 */
object GoalHorizonResolver {

    fun fromTargetDate(today: LocalDate, targetDate: LocalDate): GoalHorizon {
        // If target is in the past, treat it as "this month" (it’s urgent/overdue).
        if (!targetDate.isAfter(today)) return GoalHorizon.THIS_MONTH

        return when {
            // <= 30 days
            !targetDate.isAfter(today.plusDays(30)) -> GoalHorizon.THIS_MONTH

            // <= 6 months
            !targetDate.isAfter(today.plusMonths(6)) -> GoalHorizon.NEARTERM

            // <= 24 months
            !targetDate.isAfter(today.plusMonths(24)) -> GoalHorizon.MIDTERM

            // > 24 months
            else -> GoalHorizon.LONGTERM
        }
    }
}
