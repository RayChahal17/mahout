package com.mahout.app.domain.aim.usecase

import com.mahout.app.core.time.TimeProvider
import com.mahout.app.domain.aim.repository.ChiefAimRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Enforces the product rule:
 * - Chief Aim target date is required
 * - Must be between 5 and 20 years from "today"
 *
 * IMPORTANT: We compute "today" in the user's local timezone using ZoneId.systemDefault().
 * We do NOT call timeProvider.zoneId() because your TimeProvider doesn't expose that API.
 */
class UpsertChiefAimUseCase @Inject constructor(
    private val repo: ChiefAimRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        title: String,
        description: String?,
        targetDate: LocalDate?
    ) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "Chief Aim title is required." }

        val cleanDesc = description?.trim()?.takeIf { it.isNotBlank() }

        // Use device timezone for "today" (what users expect).
        val today: LocalDate = timeProvider.nowInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        require(targetDate != null) {
            "Chief Aim target date is required (5–20 years) so Goals can be bounded."
        }

        val min = today.plusYears(5)
        val max = today.plusYears(20)

        require(!targetDate.isBefore(min)) {
            "Chief Aim target must be at least 5 years out. This keeps your Aim truly long-term."
        }

        require(!targetDate.isAfter(max)) {
            "Chief Aim target can’t be more than 20 years out. Too far becomes too vague to plan."
        }

        // Keep your existing repository contract unchanged.
        repo.upsert(
            title = cleanTitle,
            description = cleanDesc,
            targetDate = targetDate
        )
    }
}
